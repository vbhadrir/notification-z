/*
       Copyright 2017 IBM Corp All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.portfolio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

//EJB 3.2
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

//JMS 2.0
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

//JSON-P 1.0
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Message-Driven Bean implementation class for: NotificationMDB
 */
@MessageDriven(name = "NotificationMDB", mappedName = "jms/NotificationQueue",
	activationConfig = {
		@ActivationConfigProperty(
			propertyName = "destination", propertyValue = "NotificationQ"),
		@ActivationConfigProperty(
			propertyName = "destinationType", propertyValue = "javax.jms.Queue") } )
public class NotificationMDB implements MessageListener {
	private String url = "https://openwhisk.ng.bluemix.net/api/v1/namespaces/vbhadrir%40us.ibm.com_dev/actions/PostPortfolioStatusToSlack";
	private String id = "efaf1de0-49d8-4ead-be19-9754059a5f49";
	private String pwd = "JmlDyEvRoKQzqCMgz1ia7owBrNJGEoUtpKsWYxlYFm2LZ1FnK5WW3wahyHM3Izp7";

	/**
	 * Default constructor. 
	 */
	public NotificationMDB() {
		super();

		try {
			//The following variables should be set in a Kubernetes secret, and
			//made available to the app via a stanza in the deployment yaml

			//Example secret creation command: kubectl create secret generic openwhisk
			//--from-literal=url=https://openwhisk.ng.bluemix.net/api/v1/namespaces/jalcorn%40us.ibm.com_dev/actions/PostLoyaltyLevelToSlack
			//--from-literal=id=bc2b0a37-0554-4658-9ebe-ae068eb1aa22
			//--from-literal=pwd=<myPassword>

			/* Example deployment yaml stanza:
	           spec:
	             containers:
	             - name: notification
	               image: kyleschlosser/notification:latest
	               env:
	                 - name: OW_URL
	                   valueFrom:
	                     secretKeyRef:
	                       name: openwhisk
	                       key: url
	                 - name: OW_ID
	                   valueFrom:
	                     secretKeyRef:
	                       name: openwhisk
	                       key: id
	                 - name: OW_PASSWORD
	                   valueFrom:
	                     secretKeyRef:
	                       name: openwhisk
	                       key: pwd
	               ports:
	                 - containerPort: 9080
	               imagePullPolicy: Always
	             imagePullSecrets:
	             - name: dockerhubsecret
			 */
			//url = System.getenv("OW_URL");
			//id = System.getenv("OW_ID");
			//pwd = System.getenv("OW_PASSWORD");

			if (url == null) {
				System.out.println("The OW_URL environment variable was not set!");
			} else {
				System.out.println("Initialization completed successfully!");
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * @see MessageListener#onMessage(Message)
	 */
	public void onMessage(Message message) {
		if (message instanceof TextMessage) try {
			TextMessage text = (TextMessage) message;
			String json = text.getText();

			System.out.println("Sending "+json+" to "+url);
			invokeREST("POST", url, json, id, pwd);
		} catch (Throwable t) {
			t.printStackTrace();
		} else {
			System.out.println("onMessage received a non-TextMessage!");
		}
	}

	private static JsonObject invokeREST(String verb, String uri, String input, String user, String password) throws IOException {
		URL url = new URL(uri);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(verb);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

		if ((user != null) && (password != null)) { //send via basic-auth
			String credentials = user + ":" + password;
			String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
			conn.setRequestProperty("Authorization", authorization);
		}

		if (input != null) {
			OutputStream body = conn.getOutputStream();
			body.write(input.getBytes());
			body.flush();
			body.close();
		}

		InputStream stream = conn.getInputStream();

//		JSONObject json = JSONObject.parse(stream); //JSON4J
		JsonObject json = Json.createReader(stream).readObject();

		stream.close();

		return json;
	}
}
