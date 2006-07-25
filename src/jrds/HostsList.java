/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import jrds.probe.SumProbe;

import org.apache.log4j.Logger;

/**
 * The central repository of all informations : hosts, graph, and everything else
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class HostsList {
	static Logger logger = Logger.getLogger(HostsList.class);
	private static HostsList instance;

	public static final String HOSTROOT = "Sorted by host";
	public static final String VIEWROOT = "Sorted by view";
	public static final String SUMROOT = "Sums";

	//This list is never purged
	private final static StartersSet starters = new StartersSet();

	private final Set<RdsHost> hostList = new HashSet<RdsHost>();
	private final Map<Integer, RdsGraph> graphMap = new HashMap<Integer, RdsGraph>();
	private final Map<Integer, Probe> probeMap= new HashMap<Integer, Probe>();
	private final Map<String, Macro> macroList = new HashMap<String, Macro>();
	private final Map<String, GraphTree> treeMap = new LinkedHashMap<String, GraphTree>(3);
	private final Map<String, Filter> filters = new TreeMap<String, Filter>(String.CASE_INSENSITIVE_ORDER);
	private final RdsHost sumhost =  new RdsHost("SumHost");

	/**
	 *  
	 */
	private HostsList() {
		init();
	}

	private void init() {
		addRoot(HOSTROOT);
		addRoot(VIEWROOT);
		addRoot(SUMROOT);
		filters.put(Filter.SUM.getName(), Filter.SUM);
		filters.put(Filter.EVERYTHING.getName(), Filter.EVERYTHING);
		filters.put(Filter.ALLHOSTS.getName(), Filter.ALLHOSTS);
		filters.put(Filter.ALLVIEWS.getName(), Filter.ALLVIEWS);
	}
	
	public static HostsList getRootGroup() {
		if (instance == null)
			instance = new HostsList();
		return instance;
	}
	
	/**
	 * Must be called after a new configuration has been loaded
	 */
	public void confLoaded() {
		macroList.clear();
		DescFactory.digester = null;
	}
	
	public Collection<RdsHost> getHosts() {
		return hostList;
		
	}
	
	public static void purge() {
		instance = new HostsList();
	}
	
	public void append(Collection newHostList) {
		for (Iterator i = newHostList.iterator(); i.hasNext();) {
			addHost((RdsHost) i.next());
		}
	}

	private void addRoot(String root) {
		if( ! treeMap.containsKey(root)) {
			 GraphTree newRoot = GraphTree.makeGraph(root);
			 treeMap.put(root, newRoot);
		}
	}
	
	public void addHost(RdsHost newhost) {
		hostList.add(newhost);
		for(Probe currProbe: newhost.getProbes()) {
			probeMap.put(currProbe.hashCode(), currProbe);
			if (currProbe.getGraphList() != null) {
				String rootTree = currProbe.getTree();
				if(rootTree != null)
					addRoot(rootTree);
				for(RdsGraph currGraph: currProbe.getGraphList()) {
					getGraphTreeByHost().addGraphByPath(currGraph.getTreePathByHost(), currGraph);
					getGraphTreeByView().addGraphByPath(currGraph.getTreePathByView(), currGraph);
					graphMap.put(currGraph.hashCode(), currGraph);
				}
			}
		}
		if (this != instance)
			instance.addHost(newhost);
	}

	public void collectAll() throws IOException {
		starters.startCollect();
		ExecutorService tpool =  Executors.newFixedThreadPool(PropertiesManager.getInstance().collectorThreads);
		logger.debug("One collect was launched");
		Date start = new Date();
		for(final RdsHost oneHost: hostList) {
			logger.debug("Collect all stats for host " + oneHost.getName());
			Runnable runCollect = new Runnable() {
				private RdsHost host = oneHost;
				
				public void run() {
					host.collectAll();
				}
			};
			try {
				tpool.execute(runCollect);
			}
			catch(RejectedExecutionException ex) {
				logger.debug("collector thread dropped for host " + oneHost.getName());
			}
		}
		tpool.shutdown();
		try {
			tpool.awaitTermination(PropertiesManager.getInstance().resolution - 10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("Collect interrupted");
		}
		Date end = new Date();
		long duration = end.getTime() - start.getTime();
		logger.info("Collect started at "  + start + " ran for " + duration + "ms");							
		starters.stopCollect();
	}
	
	public void graphAll(Date startDate, Date endDate) {
		for(RdsHost oneHost: hostList) {
			logger.debug("Do all graph for host " + oneHost.getName());
			oneHost.graphAll(startDate, endDate);
		}
	}
	
	public Collection<GraphTree> getGraphsRoot() {
		return treeMap.values();
	}

	public GraphTree getGraphTreeByHost() {
		return treeMap.get(HOSTROOT);
	}

	public GraphTree getGraphTreeByView() {
		return treeMap.get(VIEWROOT);
	}

	/**
	 * Return a graph identified by his hash value
	 * @param id the hash value of the graph
	 * @return the graph found or null of nothing found
	 */
	public RdsGraph getGraphById(int id) {
		return graphMap.get(id);
	}

	/**
	 * Return a probe identified by his hash value
	 * @param id the hash value of the probe
	 * @return the graph found or null of nothing found
	 */
	public Probe getProbeById(int id) {
		return probeMap.get(id);
	}

	public GraphTree getNodeByPath(String path) {
		GraphTree tree = treeMap.get(path.split("/")[1]);
		GraphTree node = null;
		if(tree != null) {
			node = tree.getByPath(path);
		}
		return node;
	}

	public GraphTree getNodeById(int id) {
		GraphTree node = null;
		for(GraphTree tree: treeMap.values())
			if(tree.getById(id) != null)
				node = tree.getById(id);
		return node;
	}

	/**
	 * @return
	 */
	public Map<String, Macro> getMacroList() {
		return macroList;
	}

	public void addStarter(Starter s) {
		starters.register(s);
	}
	
	public StartersSet getStarters() {
		return starters;
	}
	
	public void addFilter(Filter newFilter) {
		filters.put(newFilter.getName(), newFilter);
		logger.debug("Filter " + newFilter.getName() + " added");
	}
	
	public Filter getFilter(String name) {
		Filter retValue = null;
		if(name != null)
			retValue = filters.get(name);
		return retValue;
	}
	
	public Collection<String> getAllFiltersNames() {
		return filters.keySet();
	}

	public void addSum(String sumName, List<String> l) {
		SumProbe sum = new SumProbe(sumhost, sumName, l);
		for(RdsGraph currGraph: sum.getGraphList()) {
			treeMap.get(SUMROOT).addGraphByPath(currGraph.getTreePathByHost(), currGraph);
			graphMap.put(currGraph.hashCode(), currGraph);
		}
		logger.debug("adding sum " + sumName);
	}

}