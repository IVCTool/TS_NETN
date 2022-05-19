package org.nato.netn.base;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.fraunhofer.iosb.tc_lib.IVCT_TcParam;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;

public class NetnBaseTcParam implements IVCT_TcParam {

    public static final String FOMFILES = "fomFiles";

    private ArrayList<URL> urls = new ArrayList<>();

    private JSONObject parameter;

    public NetnBaseTcParam (String jsonParamString) throws TcInconclusive, ParseException {
        JSONParser parser = new JSONParser();
        parameter = (JSONObject) parser.parse(jsonParamString);

        JSONArray fileListObject = (JSONArray) parameter.get(FOMFILES);
        for (Object somFile: fileListObject) {
            try {
                urls.add((new File(somFile.toString())).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new TcInconclusive("unable to parse SOM file name", e);
            }
        }
    }

    @Override
    public URL[] getUrls() {
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }
    
}
