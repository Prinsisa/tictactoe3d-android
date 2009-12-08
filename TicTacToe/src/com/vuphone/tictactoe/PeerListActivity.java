/**
 * 
 */
package com.vuphone.tictactoe;

import java.util.ArrayList;
import java.util.Properties;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * @author Adam Albright, Ben Gotow
 * 
 */
public class PeerListActivity extends ListActivity {

	private ArrayList<String> peers;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ArrayList<Properties> peerList = GameServer.getInstance().helloList;

		if (peerList.size() == 0) {
			peers = new ArrayList<String>();
			peers.add("No opponents found!");
		} else {
			synchronized (peerList){
				peers = new ArrayList<String>();
				for (Properties p : peerList){
					peers.add(p.getProperty("name"));
				}
			}
		}

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, peers));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent i = new Intent();
		ArrayList<Properties> list = GameServer.getInstance().helloList;
		
		if (position < list.size())
			i.putExtra("ip", list.get(position).getProperty("ip"));

		setResult(RESULT_OK, i);
		finish();
	}
}
