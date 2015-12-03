/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.roboconf.messaging.http.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import net.roboconf.messaging.api.utils.SerializationUtils;
import net.roboconf.messaging.http.HttpMessage;
import net.roboconf.messaging.http.SubscriptionMessage;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
@ClientEndpoint
@ServerEndpoint(value="/messaging-http")
public class MessagingWebSocket {

	private final Logger logger = Logger.getLogger(getClass().getName());

    @OnOpen
    public void onWebSocketConnect(Session session) {
    	this.logger.finest("Socket Connected: " + session);
    }
    
    @OnMessage
    public void onWebSocketBinary(ByteBuffer buf) {
    	this.logger.finest("Received BINARY message: " + buf);

    	//HttpMessage test = new HttpMessage("test", null);
    	//this.logger.info("onWebSocketBinary TEST message=" + test);
  
    	try {
    		HttpMessage msg = SerializationUtils.deserializeObject(buf.array(), HttpMessage.class);
    		this.logger.finest("Deserialized as " + msg);
    		
    		if(msg instanceof SubscriptionMessage) {
    			manageSubscription((SubscriptionMessage)msg);
    		} else {

    			List<HttpClient> destinations = getDestinations(msg);
    			if(destinations != null) {
    				for(HttpClient client : destinations) {
    					client.addMessageToMQ(msg.getMessage());
    				}
    			}
    		}

    	} catch (ClassNotFoundException e) {
    		logger.severe("Message deserialization issue (class not found): " + e);
    	} catch (IOException e) {
    		logger.severe("Message deserialization issue: " + e);
    	}
    }
    
    @OnClose
    public void onWebSocketClose(CloseReason reason) {
    	this.logger.finest("Websocket closed: " + reason);
    }
    
    @OnError
    public void onWebSocketError(Throwable cause) {
    	this.logger.finest("Websocket error: " + cause);
    }
    
    private HttpClient findHttpClient(String id) {
    	Set<HttpClient> httpClients = HttpClientFactory.getHttpClients();
		for(HttpClient client : httpClients) {
			if(client.getId().equals(id)) return client;
		}
		return null;
    }
    
    private String buildSubscriptionKey(HttpMessage msg) {
    	return msg.getExchangeName() + (msg.getRoutingKey() == null || msg.getRoutingKey().length() <= 0 ? "" : "|" + msg.getRoutingKey());
    }

    private static Map<String, List<HttpClient>> subscriptions = new HashMap<String, List<HttpClient>>();
    private void manageSubscription(SubscriptionMessage msg) {
    	HttpClient client = findHttpClient(msg.getId());
    	String key = buildSubscriptionKey(msg);
    	synchronized(subscriptions) {
    		if(msg.isSubscribe()) {
    			List<HttpClient> clients = subscriptions.get(key);
    			if(clients == null) clients = new LinkedList<HttpClient>();
    			if(! clients.contains(client)) clients.add(client);
    			subscriptions.put(key, clients);
    		} else {
    			List<HttpClient> clients = subscriptions.get(key);
    			clients.remove(client);
    			if(clients.isEmpty()) subscriptions.remove(key);
    		}
    	}
    }
 
    private List<HttpClient> getDestinations(HttpMessage msg) {
    	List<HttpClient> destinations = new LinkedList<HttpClient>();
    	
    	// Is message intended for the DM ?
    	if(msg.getExchangeName().equals("DM")) {
    		Set<HttpClient> httpClients = HttpClientFactory.getHttpClients();
    		for(HttpClient client : httpClients) {
    			if(client instanceof HttpDmClient) destinations.add(client);
    		}
    	}
    	
    	synchronized(subscriptions) {
    		List<HttpClient> destinationsWithKey = subscriptions.get(buildSubscriptionKey(msg));
    		if(destinationsWithKey != null) destinations.addAll(destinationsWithKey);
    	}

		return destinations;
    }

    public static void cleanup() {
    	synchronized(subscriptions) { subscriptions.clear(); }
    }
}
