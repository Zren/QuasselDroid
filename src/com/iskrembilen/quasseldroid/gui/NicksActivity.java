/*
    QuasselDroid - Quassel client for Android
 	Copyright (C) 2011 Ken Børge Viktil
 	Copyright (C) 2011 Magnus Fjell
 	Copyright (C) 2011 Martin Sandsmark <martin.sandsmark@kde.org>

    This program is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version, or under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either version 2.1 of
    the License, or (at your option) any later version.

 	This program is distributed in the hope that it will be useful,
 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 	GNU General Public License for more details.

    You should have received a copy of the GNU General Public License and the
    GNU Lesser General Public License along with this program.  If not, see
    <http://www.gnu.org/licenses/>.
 */

package com.iskrembilen.quasseldroid.gui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import com.iskrembilen.quasseldroid.*;
import com.iskrembilen.quasseldroid.service.CoreConnService;
import com.iskrembilen.quasseldroid.util.ThemeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class NicksActivity extends Activity{

	private static final String TAG = NicksActivity.class.getSimpleName();
	private ResultReceiver statusReceiver;
	private NicksAdapter adapter;
	private ExpandableListView list;
	private int bufferId;
	private int currentTheme;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(ThemeUtil.theme);
		currentTheme = ThemeUtil.theme;
		setContentView(R.layout.nick_layout);

		initActionBar();
		
		Intent intent = getIntent();
		if(intent.hasExtra(ChatActivity.BUFFER_ID)) {
			bufferId = intent.getIntExtra(ChatActivity.BUFFER_ID, 0);
			Log.d(TAG, "Intent has bufferid" + bufferId);
		}

		adapter = new NicksAdapter(this);
		list = (ExpandableListView)findViewById(R.id.userList);
		list.setAdapter(adapter);
		statusReceiver = new ResultReceiver(null) {

			@Override
			protected void onReceiveResult(int resultCode, Bundle resultData) {
				if (resultCode==CoreConnService.CONNECTION_DISCONNECTED) finish();
				super.onReceiveResult(resultCode, resultData);
			}

		};
	}

	@TargetApi(14)
	private void initActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(ThemeUtil.theme != currentTheme) {
			Intent intent = new Intent(this, BufferActivity.class);
	        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
		}
		doBindService();
	}

	@Override
	protected void onStop() {
		super.onStop();
		doUnbindService();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, ChatActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(ChatActivity.BUFFER_ID, bufferId);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class NicksAdapter extends BaseExpandableListAdapter implements Observer{

		private LayoutInflater inflater;
		private UserCollection users;
		public NicksAdapter(Context context) {
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.users = null;
		}

		public void setUsers(UserCollection users) {
			users.addObserver(this);
			this.users = users;
			notifyDataSetChanged();
			for(int i=0; i<getGroupCount();i++) {
				list.expandGroup(i);
			}
		}

		@Override
		public void update(Observable observable, Object data) {
			if(data == null) {
				return;
			}
			switch ((Integer)data) {
			case R.id.BUFFERUPDATE_USERSCHANGED:
				notifyDataSetChanged();				
				break;			
			}
		}

		public void stopObserving() {
			users.deleteObserver(this);

		}

		@Override
		public IrcUser getChild(int groupPosition, int childPosition) {
			return getGroup(groupPosition).second.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
            //TODO: This will cause bugs when you have more than 99 children in a group
			return groupPosition*100 + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

			ViewHolderChild holder = null;

			if (convertView==null) {
				convertView = inflater.inflate(R.layout.nicklist_item, null);
				holder = new ViewHolderChild();
				holder.nickView = (TextView)convertView.findViewById(R.id.nicklist_nick_view);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolderChild)convertView.getTag();
			}
			IrcUser entry = getChild(groupPosition, childPosition);
			holder.nickView.setText(entry.nick);
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (this.users==null) return 0;
			return getGroup(groupPosition).second.size();
		}

		@Override
		public Pair<IrcMode,List<IrcUser>> getGroup(int groupPosition) {
            int counter = 0;
			for(IrcMode mode: IrcMode.values()){
                if (counter == groupPosition){
                    return new Pair<IrcMode, List<IrcUser>>(mode,users.getUniqueUsersSortedByMode().get(mode));
                } else {
                    counter++;
                }
            }
            return null;
		}

		@Override
		public int getGroupCount() {
			return IrcMode.values().length;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			ViewHolderGroup holder = null;

			if (convertView==null) {
				convertView = inflater.inflate(R.layout.nicklist_group_item, null);
				holder = new ViewHolderGroup();
				holder.nameView = (TextView)convertView.findViewById(R.id.nicklist_group_name_view);
				holder.imageView = (ImageView)convertView.findViewById(R.id.nicklist_group_image_view);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolderGroup)convertView.getTag();
			}
            Pair<IrcMode, List<IrcUser>> group = getGroup(groupPosition);
            if(group.second.size()>2){
                holder.nameView.setText(group.second.size() + " "+group.first.modeName+group.first.pluralization);
            } else {
                holder.nameView.setText(group.second.size() + " "+group.first.modeName);
            }
            holder.imageView.setImageResource(group.first.iconResource);
            if(group.second.size()<1){
                //TODO: Make group invisible if it does not have any children
            }
            return convertView;
        }

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
	}


	public static class ViewHolderChild {
		public TextView nickView;
	}
	public static class ViewHolderGroup {
		public TextView nameView;
		public ImageView imageView;
	}


	/**
	 * Code for service binding:
	 */
	private CoreConnService boundConnService;
	private Boolean isBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			Log.d(TAG, "BINDING ON SERVICE DONE");
			boundConnService = ((CoreConnService.LocalBinder)service).getService();

			Buffer buffer = boundConnService.getBuffer(bufferId, null);
			adapter.setUsers(buffer.getUsers());

			boundConnService.registerStatusReceiver(statusReceiver);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			boundConnService = null;

		}
	};

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(NicksActivity.this, CoreConnService.class), mConnection, Context.BIND_AUTO_CREATE);
		isBound = true;
		Log.i(TAG, "BINDING");
	}

	void doUnbindService() {
		if (isBound) {
			Log.i(TAG, "Unbinding service");
			// Detach our existing connection.
			adapter.stopObserving();
			boundConnService.unregisterStatusReceiver(statusReceiver);
			unbindService(mConnection);
			isBound = false;

		}
	}
}
