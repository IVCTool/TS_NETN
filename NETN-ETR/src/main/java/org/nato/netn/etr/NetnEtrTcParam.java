package org.nato.netn.etr;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

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
    public static final String SUT_TASK_ID = "taskId";
    
    private NetnFomFiles fomFiles;
    //private ArrayList<URL> urls = new ArrayList<>();

    private JSONObject parameter;

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
        fomFiles.addNetnBase().addNetnEtr().addNetnSmc();
    }

    @Override
    public URL[] getUrls() {
        //eturn (URL[]) urls.toArray(new URL[urls.size()]);
        return fomFiles.get();
    }

    public String getSutFederateName() throws TcInconclusive {
        Object o = parameter.get(SUT_FEDERATE_NAME);
        if (o == null) throw new TcInconclusive("Parameter " + SUT_FEDERATE_NAME + " not set.");
        return (String)o;
    }

    public String getFederationName() throws TcInconclusive {
        Object o = parameter.get(FEDERATION_NAME);
        if (o == null) {
            throw new TcInconclusive("Parameter " + FEDERATION_NAME + " not set.");
        }
        return (String)o;
    }

    public String [] getSupportedActions() throws TcInconclusive {
        Object o = parameter.get(SUT_SUPPRTED_ACTIONS);
        if (o == null) {
            throw new TcInconclusive("Parameter " + SUT_SUPPRTED_ACTIONS + " not set.");
        }
        JSONArray ja = (JSONArray)o;
        return Arrays.stream(ja.toArray()).map(t -> (String)t).toArray(String[]::new);
    }

    public String getTaskId() throws TcInconclusive {
        Object o = parameter.get(SUT_TASK_ID);
        if (o == null) {
            throw new TcInconclusive("Parameter " + SUT_TASK_ID + " not set.");
        }
        return (String)o;        
    }

}
