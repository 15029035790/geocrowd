package org.geocrowd.matching.online;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class OnlineBipartiteMatching {
	public HashMap<Integer, Integer> workers = null;		// <order id, worker idx>
	
	public ArrayList<Integer> ranks;		// point to order id. the smaller index (index in workers), the higher rank
	
	/**
	 * Initialize variables
	 * @param workers
	 */
	public OnlineBipartiteMatching(ArrayList<Integer> _workers) {
		java.util.Collections.shuffle(_workers); // permute workers
		
		workers = new HashMap<>();
		 Iterator<Integer> it = _workers.iterator();
		 int j= 0; 
		 while (it.hasNext()) {
			 Integer i = it.next();
			 workers.put(j, i);
			 j++;
		 }
		 

		 // assign random rank for each worker
		 ranking();
	}
	
	/**
	 * Rank the workers
	 */
	public void ranking() {
		ranks = new ArrayList<Integer>();
		 for (int i = 0; i < workers.size(); i++) {
			 ranks.add(new Integer(i));
		 }
		 
		 java.util.Collections.shuffle(ranks);
	}
	

	/**
	 * Online algorithm
	 * @param container: an array of worker ids
	 * @return the number of assigned tasks
	 */
	public ArrayList<Integer> onlineMatching(HashMap<Integer, ArrayList> invertedContainer) {
		ArrayList<Integer> assignedTasks = new ArrayList<>();
		Iterator it = invertedContainer.keySet().iterator();
		
		// iterate through task list
		while (it.hasNext() && workers.size() > 0) {
			Integer taskidx = (Integer) it.next();
			ArrayList<Integer> workerids = invertedContainer.get(taskidx);	//	list of workers eligible to perform this task
			
			// put all workerids into a hashset
			HashSet<Integer> hashids = new HashSet<Integer>();
			for (int i = 0; i < workerids.size(); i++) {
				hashids.add(workerids.get(i));
			}
			
			// find the worker of highest rank by iterate through ranks
			for (int i = 0; i < ranks.size(); i++) {
				if (hashids.contains(workers.get(ranks.get(i)))) {
					assignedTasks.add(taskidx);
					
					// remove the task & rank from workers and ranks
					workers.remove(ranks.get(i));
					ranks.remove(i);
					break; // find the worker
				}
			}
		}
		
		return assignedTasks;
	}
	
}