package gossip_protocol.src;

import java.util.*;
import java.util.Random;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GossipperThread implements Runnable {

	protected HashMap<String, Member> alliances;
	private Member _thisMember;
	private Socket _gossipperSocket;
	private PrintWriter _out;
	private final AtomicBoolean _isHealthy;

	public GossipperThread(Member thisMember, HashMap<String, Member> alliances, AtomicBoolean isHealthy) {
		_thisMember = thisMember;
		this.alliances = alliances;
		_isHealthy = isHealthy;
	}

	public String createGossipDigest() {
		StringBuilder digestBuilder = new StringBuilder();
		digestBuilder.append("gossip ");
		/*
		for (Member member : alliances) {
			digestBuilder.append(member.toString() + "-");
		}
		*/
		synchronized(this) {
			for (Map.Entry<String, Member> entry : alliances.entrySet()) {
				digestBuilder.append(entry.getValue().toString() + "-");
			}
		}
		return  digestBuilder.toString() + "_" + _thisMember.getAddress() + ":" + String.valueOf(_thisMember.getPort());
	}

	public void sendGossip() {
		_thisMember.incrementHeartbeat();

		String gossipDigest = createGossipDigest();
		//Collections.shuffle(alliances);

		try {
			//TODO select 3 random nodes from the hashmap
			//TODO randomly select 3 indices in hashmap and gossip to them
			//TODO create a configuration file that reads in how many nodes to randomly gossip to periodically

			HashSet<Integer> randomSet = new HashSet<Integer>();

			if (alliances.size() == 1) {
				randomSet.add(0);
			} else if (alliances.size() == 2) {
				randomSet.add(0);
				randomSet.add(1);
			} else if (alliances.size() == 3) {
				randomSet.add(0);
				randomSet.add(1);
				randomSet.add(2);
			} else {
				Random rand = new Random();
				int index1 = rand.nextInt(alliances.size());
				int index2 = rand.nextInt(alliances.size());
				int index3 = rand.nextInt(alliances.size()); 
				randomSet.add(index1);
				randomSet.add(index2);
				randomSet.add(index3);
			}

			/*
			for (int i = 0; i < alliances.size(); i++) {
				if (i > 3) { break; }
				_gossipperSocket = new Socket(alliances.get(i).getAddress(), alliances.get(i).getPort());
				_out = new PrintWriter(_gossipperSocket.getOutputStream());
				_out.println(gossipDigest);
				_out.close();
			}
			*/
			int selector = 0;
			synchronized(this) {
				for (Map.Entry<String, Member> entry : alliances.entrySet()) {
					if (randomSet.contains(selector)){
						_gossipperSocket = new Socket(entry.getValue().getAddress(), entry.getValue().getPort());
						_out = new PrintWriter(_gossipperSocket.getOutputStream());
						_out.println(gossipDigest);
						_out.close();
					}
					selector++;
				}
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		Logger.info("Starting gossiper thread...");
		try {
			while (_isHealthy.get()) {
				Thread.sleep(3000);
				Logger.info("Sending gossip...");
				sendGossip();
				Logger.info("Gossip sent");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Logger.info("Finished gossiper thread.");
	}

}