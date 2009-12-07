/**
 * 
 */
package com.vuphone.tictactoe;

import java.util.ArrayList;
import java.util.Collection;

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

		Collection<String> peerList = GameServer.getInstance().helloList
				.values();

		if (peerList.size() == 0) {
			peers = new ArrayList<String>();
			peers.add("No opponents found!");
		} else {
			peers = new ArrayList<String>(peerList);
		}

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, peers));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent i = new Intent();
		i.putExtra("ip", "ip-addy");

		setResult(RESULT_OK, i);
		finish();
	}
}
