// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// 
// Automatically generated by addcopyright.py at 04/03/2012
package com.cloud.test.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cloud.sample.Base64;
import com.cloud.test.demo.response.CloudStackIpAddress;
import com.cloud.test.demo.response.CloudStackPortForwardingRule;
import com.cloud.test.demo.response.CloudStackUserVm;
import com.cloud.utils.PropertiesUtil;
import com.google.gson.reflect.TypeToken;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

/**
 * @author Alena Prokharchyk
 */
public class Demo {
    private static HashMap<String, Boolean> _apiCommands = new HashMap<String, Boolean>();
    private static Properties properties = new Properties();
    private static String _host = null;

    public static void main(String[] args){
        //read properties files
        readProperties();
        _host = "http://" + properties.getProperty("hostname") + "/client/api?";
        //create the deployment
        String publicIp = createDeployment();
        //setup web server on user vm
        setupHttpd(publicIp, "cloud.com");
        
        //return web access url
        System.out.println("\nApache server is running on " + "http://" + publicIp);
    }
    
    private static void readProperties() {
        Properties preProcessedCommands = new Properties();
        String filePath = "../conf/demo/commandstype.properties";
        File configFile = PropertiesUtil.findConfigFile(filePath);
        try {
            if (configFile != null) {
                preProcessedCommands.load(new FileInputStream(configFile));
                for (Object key : preProcessedCommands.keySet()) {
                    String strKey = (String)key;
                    String asyncParams = (String)preProcessedCommands.getProperty(strKey);
                    boolean isAsync = false;
                    if (String.valueOf(asyncParams).equalsIgnoreCase("async")) {
                        isAsync=true;
                    }
                    _apiCommands.put(strKey, isAsync);
                }
            } 
        } catch (FileNotFoundException fnfex) {
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("ERROR: Error reading properites file " + filePath);
            System.exit(1);      
        } finally {
            if (configFile == null) {
                System.out.println("ERROR: Unable to find properites file " + filePath);
            }
        }
        
        filePath = "../conf/demo/setup.properties";
        configFile = PropertiesUtil.findConfigFile(filePath);
        try {
            if (configFile != null) {
                properties.load(new FileInputStream(configFile));
            } 
        } catch (FileNotFoundException fnfex) {
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("ERROR: Error reading properites file " + filePath);
            System.exit(1);      
        } finally {
            if (configFile == null) {
                System.out.println("ERROR: Unable to find properites file " + filePath);
            }
        }
    }

    
    private static void setupHttpd(String host, String password) {
        if (host == null) {
            System.out.println("Did not receive a host back from test, ignoring ssh test");
            System.exit(1);
        }

        if (password == null) {
            System.out.println("Did not receive a password back from test, ignoring ssh test");
            System.exit(1);
        }

        try {  
            System.out.println("\nSleeping for 1 min before trying to ssh into linux host ");
            Thread.sleep(60000);
            System.out.println("\nAttempting to SSH into linux host " + host);

            Connection conn = new Connection(host);
            conn.connect(null, 60000, 60000);

            System.out.println("User root ssHed successfully into linux host " + host);

            boolean isAuthenticated = conn.authenticateWithPassword("root", password);

            if (isAuthenticated == false) {
                System.out.println("ERROR: Authentication failed for root with password" + password);
                System.exit(1);
            }

            boolean success = false;
            String linuxCommand = "yum install httpd -y && service httpd start && service iptables stop";
            //String linuxCommand = "yum install tomcat* -y && service iptables stop && /etc/init.d/tomcat5 start";

            Session sess = conn.openSession();
            System.out.println("User root executing : " + linuxCommand);
            sess.execCommand(linuxCommand);

            InputStream stdout = sess.getStdout();
            InputStream stderr = sess.getStderr();

            byte[] buffer = new byte[8192];
            while (true) {
                if ((stdout.available() == 0) && (stderr.available() == 0)) {
                    int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 120000);

                    if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                        System.out.println("ERROR: Timeout while waiting for data from peer.");
                        System.exit(1);
                    }

                    if ((conditions & ChannelCondition.EOF) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }
                }

                while (stdout.available() > 0) {
                    success = true;
                    int len = stdout.read(buffer);
                    if (len > 0) 
                        System.out.println(new String(buffer, 0, len));
                }

                while (stderr.available() > 0) {
                    /* int len = */stderr.read(buffer);
                }
            }

            sess.close();
            conn.close();

            if (!success) {
                System.out.println("ERROR: SSH Linux Network test failed: unable to setup httpd");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("ERROR: SSH Linux Network test fail with error " + e.getMessage());
            System.exit(1);
        }
        System.out.println("Apache is setup succesfully on the user vm");
    }
    
    public static String createDeployment() {
        System.out.println("\nCreating cloudStack deployment...");
        CloudStackHttpClient client = new CloudStackHttpClient();
        
        //1) deployVm
        String urlToSign = "command=deployVirtualMachine&serviceOfferingId=" + properties.getProperty("serviceOfferingId") + 
                "&networkIds=" + properties.getProperty("networkId") + 
                "&templateId=" + properties.getProperty("templateId") + "&zoneId=" + properties.getProperty("zoneId") + 
                "&response=json";
        String url = signRequest(urlToSign, properties.getProperty("apikey"),
                properties.getProperty("secretkey"));
        String requestUrl = _host + url;
        
        
        CloudStackUserVm vm = client.execute(requestUrl, _apiCommands.get("deployVirtualMachine"), "deployvirtualmachineresponse", 
                "virtualmachine", CloudStackUserVm.class);
        
        String vmId = null;
        if(vm != null){
            vmId = vm.getId();
            System.out.println("\nDeployed vm with id " + vmId);
        } else {
            System.out.println("ERROR: failed to deploy the vm");
            System.exit(1);
        }
        
        //2) List public IP address - source nat
        urlToSign = "command=listPublicIpAddresses&zoneId=" + properties.getProperty("zoneId") + "&response=json";
        url = signRequest(urlToSign, properties.getProperty("apikey"), 
                properties.getProperty("secretkey"));
        requestUrl = _host + url;
        
        List<CloudStackIpAddress> ipList = client.execute(requestUrl,"listpublicipaddressesresponse", "publicipaddress", 
                new TypeToken<List<CloudStackIpAddress>>(){}.getType());
        
        if (ipList.isEmpty()) {
            System.out.println("ERROR: account doesn't own any public ip address");
            System.exit(1);
        } 
        
        CloudStackIpAddress ip = ipList.get(0);
        String ipId = ip.getId();
        String ipAddress = ip.getIpAddress();
        
        
        //3) create portForwarding rules for port 22 and 80
        urlToSign = "command=createPortForwardingRule&privateport=22&publicport=22&protocol=tcp&ipaddressid=" + ipId + 
                "&virtualmachineid=" + vmId + "&response=json";
        url = signRequest(urlToSign, properties.getProperty("apikey"), 
                properties.getProperty("secretkey"));
        requestUrl = _host + url;
        CloudStackPortForwardingRule pfrule1 = client.execute(requestUrl, _apiCommands.get("createPortForwardingRule"), 
                "createportforwardingruleresponse", "portforwardingrule", CloudStackPortForwardingRule.class);
        
        if (pfrule1 == null) {
            System.out.println("ERROR: failed to create pf rule for the port 22");
            System.exit(1);
        }
        
        urlToSign = "command=createPortForwardingRule&privateport=80&publicport=80&protocol=tcp&ipaddressid=" + ipId + 
                "&virtualmachineid=" + vmId + "&response=json";
        url = signRequest(urlToSign, properties.getProperty("apikey"), 
                properties.getProperty("secretkey"));

        requestUrl = _host + url;
        CloudStackPortForwardingRule pfrule2 = client.execute(requestUrl, _apiCommands.get("createPortForwardingRule"), 
                "createportforwardingruleresponse", "portforwardingrule", CloudStackPortForwardingRule.class);

        if (pfrule2 == null) {
            System.out.println("ERROR: failed to create pf rule for the port 80");
            System.exit(1);
        }
        
        System.out.println("\nCreated PF rules for ports 22 and 80 ");
        System.out.println("\nCloudStack finished the deployment ");
        
        return ipAddress;
    }
    
    
    public static String generateSignature(String request, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(),
                    "HmacSHA1");
            mac.init(keySpec);
            mac.update(request.getBytes());
            byte[] encryptedBytes = mac.doFinal();
            return Base64.encodeBytes(encryptedBytes);
        } catch (Exception ex) {
            System.out.println("unable to sign request due to exception " + ex.getMessage());
            System.exit(1);
        }
        return null;
    }
    
    public static String signRequest(String request, String apiKey, String secretKey) {
        
        //sorted map (sort by key)
        TreeMap<String, String> param = new TreeMap<String, String>();
        
        String finalRequest = "";
        param.put("apikey", apiKey);
        
        //1) Parse the URL and put all parameters to sorted map
        StringTokenizer str1 = new StringTokenizer (request, "&");
        while(str1.hasMoreTokens()) {
            String newEl = str1.nextToken();
            StringTokenizer str2 = new StringTokenizer(newEl, "=");
            String name = str2.nextToken();
            String value= str2.nextToken();
            param.put(name, value);
        }
        
        //2) Go through sorted map, pull out the parameters, form the url and URL encode parameters' values
        Set<Map.Entry<String, String>> c = param.entrySet();
        Iterator<Map.Entry<String,String>> it = c.iterator();
        boolean firstElement = true;
        while (it.hasNext()) {
            if (!firstElement) {
                finalRequest = finalRequest + "&";
            }
            Map.Entry<String, String> me = (Map.Entry<String, String>)it.next();
            String key = (String) me.getKey();
            String value = (String) me.getValue();
            try {
                //url encode values only
                finalRequest = finalRequest + key + "=" + URLEncoder.encode(value, "UTF-8");
            } catch (Exception ex) {
                System.out.println("Unable to set parameter " + value + " for the command " + param.get("command"));
                System.exit(1);
            }
            firstElement = false;
        }
        
        //3) Lower case the request
        String requestToSign = finalRequest.toLowerCase();
        
        //4) Generate the signature
        String signature = generateSignature(requestToSign, secretKey);
        
        //5) Encode the signature
        String encodedSignature = "";
        try {
            encodedSignature = URLEncoder.encode(signature, "UTF-8");
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }
        
        //6) append the signature to the url
        request = finalRequest + "&signature=" + encodedSignature;
        return request;
    }
    
    public static String getQueryAsyncCommandString(String jobId){
        String req = "command=queryAsyncJobResult&response=json&jobId=" + jobId;
        String url = signRequest(req, properties.getProperty("apikey"),
                properties.getProperty("secretkey"));
        String requestUrl = _host + url;
        return requestUrl;
    }
    
}

