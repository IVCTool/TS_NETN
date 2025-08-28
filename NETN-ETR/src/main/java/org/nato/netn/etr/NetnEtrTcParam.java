package org.nato.netn.etr;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.fraunhofer.iosb.tc_lib.IVCT_TcParam;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;

import org.nato.ivct.OmtEncodingHelpers.Netn.NetnFomFiles;

public class NetnEtrTcParam implements IVCT_TcParam {

    //public static final String FOMFILES = "fomFiles";
    public static final String FEDERATION_NAME = "federationName";
    public static final String SUT_FEDERATE_NAME = "sutFederateName";
    public static final String SUT_SUPPRTED_ACTIONS = "supportedActions";
    public static final String SUT_PUBLISHED_TASKS = "publishedTasks";
    public static final String SUT_SUSCRIBED_REPORTS = "subscribedReports";
    public static final String SUT_TASK_ID = "taskId";
    public static final String ROUTE = "route";
    public static final String SPEED = "speed";
    public static final String CANCEL_TASK = "cancelTask";
    public static final String SELF_TEST = "selfTest";
    
    private NetnFomFiles fomFiles;

    private JSONObject parameter;

    public class Point2D {
        private double x;
        private double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
        public double getX() {
            return x;
        }
        public void setX(double x) {
            this.x = x;
        }
        public double getY() {
            return y;
        }
        public void setY(double y) {
            this.y = y;
        }
        @Override
        public String toString() {
            return "(x: " + x + ", y: " + y + ")";
        }
    }

    public NetnEtrTcParam (String jsonParamString) throws TcInconclusive, ParseException, IOException {

        JSONParser parser = new JSONParser();
        parameter = (JSONObject) parser.parse(jsonParamString);
        // get additional parameters from json configuration
        // parameter.get(...);

        // JSONArray fileListObject = (JSONArray) parameter.get(FOMFILES);
        // for (Object somFile: fileListObject) {
        //     try {
        //         urls.add((new File(somFile.toString())).toURI().toURL());
        //     } catch (MalformedURLException e) {
        //         throw new TcInconclusive("unable to parse SOM file name", e);
        //     }
        // }
        fomFiles = new NetnFomFiles();
        fomFiles.addNetnBase().addNetnEtr().addNetnSmc().addRPR_BASE();
    }

    @Override
    public URL[] getUrls() {
        //eturn (URL[]) urls.toArray(new URL[urls.size()]);
        return fomFiles.get();
    }

    private String getString(String pn)  throws TcInconclusive {
        Object o = parameter.get(pn);
        if (o == null) throw new TcInconclusive("Parameter " + pn + " not set.");
        return (String)o;
    }

    private boolean getBoolean(String pn) throws TcInconclusive {
        Object o = parameter.get(pn);
        if (o == null) {
            throw new TcInconclusive("Parameter " + pn + " not set.");
        }        
        return ((Boolean)o).booleanValue();
    }

    private String [] getSStringArray(String pn) throws TcInconclusive {
        Object o = parameter.get(pn);
        if (o == null) {
            throw new TcInconclusive("Parameter " + pn + " not set.");
        }
        JSONArray ja = (JSONArray)o;
        return Arrays.stream(ja.toArray()).map(t -> (String)t).toArray(String[]::new);
    }

    public String getSutFederateName() throws TcInconclusive {
        return getString(SUT_FEDERATE_NAME);
    }

    public String getFederationName() throws TcInconclusive {
        return getString(FEDERATION_NAME);
    }

    public String [] getSupportedActions() throws TcInconclusive {
        return getSStringArray(SUT_SUPPRTED_ACTIONS);
    }

    public String [] getPublishedTasks() throws TcInconclusive {
        return getSStringArray(SUT_PUBLISHED_TASKS);
    }

    public String [] getSubscribedReports() throws TcInconclusive {
        return getSStringArray(SUT_SUSCRIBED_REPORTS);
    }

    public String getTaskId() throws TcInconclusive {
        return getString(SUT_TASK_ID);
    }

    public float getSpeed() throws TcInconclusive {
        Object o = parameter.get(SPEED);
        if (o == null) {
            throw new TcInconclusive("Parameter " + SPEED + " not set.");
        }
        return((Number)o).floatValue();        
    }

    public List<Point2D> getWaypoints() throws TcInconclusive {
        Object o = parameter.get(ROUTE);
        if (o == null) {
            throw new TcInconclusive("Parameter " + ROUTE + " not set.");
        }
        JSONArray ja = (JSONArray)o;
        return Arrays.stream(ja.toArray()).map(t -> {
            JSONObject jo = (JSONObject)t;
            double x = ((Number)jo.get("x")).doubleValue();
            double y = ((Number)jo.get("y")).doubleValue();
            return new Point2D(x,y);
        }).collect(Collectors.toList());
    }

    public boolean getSelfTest() throws TcInconclusive {
        return getBoolean(SELF_TEST);
    }

    public boolean getCancelTask() throws TcInconclusive {
        return getBoolean(CANCEL_TASK);
    }    
}
