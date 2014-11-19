package jrds;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdFileBackendFactory;

/**
 * A wrapper classe, to manage the rrdDb operations
 */
public final class StoreOpener {
    static final private Logger logger = Logger.getLogger(StoreOpener.class);

    private static RrdDbPool instance = null;

    private static final AtomicLong waitTime = new AtomicLong(0);
    private static final AtomicInteger lockCount = new AtomicInteger(0);
    private static RrdBackendFactory backend;
    private static boolean usepool = false;

    /**
     * Retrieves the RrdDb instance matching a specific RRD datasource name
     * (usually a file name) and using a specified RrdBackendFactory.
     *
     * @param rrdFile Name of the RRD datasource.
     * @return RrdDb instance of the datasource.
     * @throws IOException Thrown in case of I/O error.
     */
    public final static RrdDb getRrd(String rrdFile)
            throws IOException {
        File f = new File(rrdFile);
        String cp = f.getCanonicalPath();
        long start = System.currentTimeMillis();
        RrdDb db;
        if(usepool)
            db = instance.requestRrdDb(cp);
        else
            db = new RrdDb(cp, backend);
        long finish = System.currentTimeMillis();
        waitTime.addAndGet(finish - start);
        lockCount.incrementAndGet();
        return db;
    }

    /**
     * @param db
     */
    public final static void releaseRrd(RrdDb db)  {
        try {
            long start = System.currentTimeMillis();
            if(usepool)
                instance.release(db);
            else
                db.close();
            long finish = System.currentTimeMillis();
            waitTime.addAndGet(finish - start);
            lockCount.incrementAndGet();
        } catch (Exception e) {
            logger.debug("Strange error " + e);
        }
    }

    public static final void prepare(String backend) {
        usepool = false;
        if(backend != null) {
            StoreOpener.backend = RrdBackendFactory.getFactory(backend);
            logger.trace(Util.delayedFormatString("Store backend set to %s", backend));
        }
        else
            StoreOpener.backend = RrdBackendFactory.getDefaultFactory();

        logger.debug(Util.delayedFormatString("Store backend used is %s",  StoreOpener.backend.getName()));
        logger.debug(Util.delayedFormatString("use pool: %b",  usepool));
    }

    public static final void prepare(String backend, int dbPoolSize) {
        usepool = false;
        if(backend != null) {
            try {
                RrdBackendFactory.setDefaultFactory(backend);
                logger.trace(Util.delayedFormatString("Store backend set to %s", backend));
            } catch (IllegalArgumentException e) {
                logger.fatal("Backend not configured: " + e.getMessage());
            } catch (IllegalStateException e) {
                logger.warn("Trying to change default backend, a restart is needed");
            }
        }
        StoreOpener.backend = RrdBackendFactory.getDefaultFactory();

        if(StoreOpener.backend instanceof RrdFileBackendFactory && dbPoolSize != 0) {
            try {
                instance = RrdDbPool.getInstance();
                instance.setCapacity(dbPoolSize);
                usepool = true;
            } catch (Exception e) {
            }
        }
        logger.debug(Util.delayedFormatString("Store backend used is %s",  StoreOpener.backend.getName()));
        logger.debug(Util.delayedFormatString("use pool: %b %d",  usepool, dbPoolSize));
    }

    public static final void stop() {
        logger.info("Average wait time: " +  waitTime.doubleValue() / lockCount.doubleValue() + " ms");

    }

    public static final void reset() {
    }

    /**
     * @return the instance
     */
    public static RrdDbPool getInstance() {
        return instance;
    }

    /**
     * @param rrdDb
     * @return
     * @throws IOException
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(org.rrd4j.core.RrdDb)
     */
    public static int getOpenCount(RrdDb rrdDb) throws IOException {
        return instance.getOpenCount(rrdDb);
    }

    /**
     * @param path
     * @return
     * @throws IOException
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(java.lang.String)
     */
    public static int getOpenCount(String path) throws IOException {
        return instance.getOpenCount(path);
    }
}
