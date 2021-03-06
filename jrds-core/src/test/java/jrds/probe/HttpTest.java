package jrds.probe;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Tools;
import jrds.Util;
import jrds.starter.HostStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpTest {
    static final private Logger logger = Logger.getLogger(HttpTest.class);

    static final private String HOST = "testhost";
    static final private HostStarter webserver = new HostStarter(new HostInfo(HOST));

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.Util");
    }

    private void validateBean(HttpProbe<String> p) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Assert.assertEquals("invalid url bean", p.getUrl(), p.getPd().getBean("url").get(p));
        Assert.assertEquals("invalid port bean", p.getPort(), p.getPd().getBean("port").get(p));
        Assert.assertEquals("invalid file bean", p.getFile(), p.getPd().getBean("file").get(p));

        Assert.assertEquals("invalid url bean template", p.getUrl().toString(), Util.parseTemplate("${attr.url}", p));
        Assert.assertEquals("invalid port bean template", p.getPort().toString(), Util.parseTemplate("${attr.port}", p));
        Assert.assertEquals("invalid file bean template", p.getFile(), Util.parseTemplate("${attr.file}", p));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build1() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HttpProbe<String> p = new HttpProbe<String>() {
            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                return null;
            }
        };
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setHost(webserver);
        p.setPd(pd);
        p.setFile("/");
        p.setPort(80);
        Assert.assertTrue(p.configure());
        Assert.assertEquals("http://" + HOST + ":80/", p.getUrlAsString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build2() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HttpProbe<String> p = new HttpProbe<String>() {
            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                return null;
            }
        };
        p.setHost(webserver);
        ProbeDesc<String> pd = new ProbeDesc<String>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setPd(pd);
        p.setFile("/file");
        p.setPort(80);
        Assert.assertTrue(p.configure("/file"));
        Assert.assertEquals("http://" + HOST + ":80/file", p.getUrlAsString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build3() throws IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HttpProbe<String> p = new HttpProbe<String>() {
            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                return null;
            }
        };
        p.setHost(webserver);
        ProbeDesc<String> pd = new ProbeDesc<String>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setPd(pd);
        p.setFile("/file");
        p.setPort(81);
        Assert.assertTrue(p.configure());
        Assert.assertEquals("http://" + HOST + ":81/file", p.getUrlAsString());
        Assert.assertEquals("http://" + HOST + ":81/file", pd.getBean("url").get(p).toString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build4() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HttpProbe<String> p = new HttpProbe<String>() {
            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                return null;
            }
        };
        ProbeDesc<String> pd = new ProbeDesc<String>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setHost(webserver);
        p.setPd(pd);
        p.setFile("/");
        p.setPort(80);
        p.setLogin("login@domain");
        p.setPassword("password");
        Assert.assertTrue(p.configure());
        Assert.assertEquals("http://login%40domain:password@" + HOST + ":80/", p.getUrlAsString());
        validateBean(p);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void build5() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HttpProbe<String> p = new HttpProbe<String>() {
            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                return null;
            }
        };
        ProbeDesc<String> pd = new ProbeDesc<String>();
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        p.setHost(webserver);
        p.setPd(pd);
        p.setFile("/${1}");
        p.setPort(80);
        List<Object> args = Arrays.asList((Object) "file");
        Assert.assertTrue(p.configure(args));
        Assert.assertEquals("http://" + HOST + ":80/file", p.getUrlAsString());
        validateBean(p);
    }

}
