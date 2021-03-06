package jrds.configuration;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import jrds.HostInfo;
import jrds.Macro;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.mockobjects.MokeProbeFactory;

public class TestSnmpLoadConfiguration {

    static final private Logger logger = Logger.getLogger(TestSnmpLoadConfiguration.class);

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.setLevel(logger, Level.TRACE, "jrds", "jrds.configuration", "jrds.Probe.DummyProbe", "jrds.snmp");
        Tools.configure();
        Tools.prepareXml(false);
        Logger.getLogger("jrds.factories.xml.CompiledXPath").setLevel(Level.INFO);
    }

    private ConfigObjectFactory prepare(PropertiesManager pm) throws IOException {
        ConfigObjectFactory conf = new ConfigObjectFactory(pm);
        conf.setGraphDescMap();
        conf.setProbeDescMap();
        return conf;
    }

    @Test
    public void testHost() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);
        Tools.findDescs(pm);
        ConfigObjectFactory conf = prepare(pm);
        Map<String, JrdsDocument> hostDescMap = new HashMap<String, JrdsDocument>();
        conf.getLoader().setRepository(ConfigType.HOSTS, hostDescMap);

        JrdsDocument hostNode = Tools.parseRessource("goodhost1.xml");
        String tagname = "mytag";
        JrdsElement je = hostNode.getRootElement();
        je.addElement("tag").addTextNode(tagname);
        je.addElement("snmp", "community=public", "version=2");
        hostDescMap.put("name", hostNode);
        Map<String, HostInfo> hostMap = conf.setHostMap(Tools.getSimpleTimerMap());

        logger.trace(hostMap);
        HostInfo h = hostMap.get("myhost");
        Assert.assertNotNull(h);
        Assert.assertEquals("myhost", h.getName());
        Collection<Probe<?, ?>> probes = new HashSet<Probe<?, ?>>();
        for(Probe<?, ?> p: h.getProbes())
            probes.add(p);
        Assert.assertEquals(7, h.getNumProbes());
        Assert.assertTrue("tag not found", h.getTags().contains(tagname));
        Assert.assertEquals("SNMP starter not found", "jrds.snmp.SnmpConnection", h.getConnection("jrds.snmp.SnmpConnection").getName());
    }

    @Test
    public void testRecursiveMacro() throws Exception {
        JrdsDocument d = Tools.parseString(TestLoadConfiguration.goodMacroXml);
        String tagname = "mytag";

        JrdsElement root = d.getRootElement();
        root.addElement("tag").addTextNode(tagname);
        root.addElement("snmp", "community=public", "version=2");

        MacroBuilder b = new MacroBuilder();

        Macro m = b.makeMacro(d);

        Map<String, Macro> macroMap = new HashMap<String, Macro>();
        macroMap.put(m.getName(), m);

        JrdsDocument hostdoc = Tools.parseString(TestLoadConfiguration.goodHostXml);
        hostdoc.getRootElement().addElement("macro", "name=macrodef");
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

        HostInfo host = hb.makeHost(hostdoc);
        logger.trace("dns name: " + host.getName());
        Assert.assertTrue("tag not found", host.getTags().contains(tagname));
        Assert.assertEquals("SNMP starter not found", "jrds.snmp.SnmpConnection", host.getConnection("jrds.snmp.SnmpConnection").getName());
    }

}
