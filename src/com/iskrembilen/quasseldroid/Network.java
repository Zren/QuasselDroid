package com.iskrembilen.quasseldroid;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class Network extends Observable implements Observer, Comparable<Network> {
	private int networkId;
	private Buffer statusBuffer;
	private String networkName;
	private Boolean isConnected;
	private BufferCollection buffers;
	private List<IrcUser> userList;
	private String nick;
	
	private boolean open;

	


	public Network(int networkId) {
		this.networkId = networkId;
		userList = new ArrayList<IrcUser>();
		buffers = new BufferCollection();
		
		open=true;
	}
	
	
	public Buffer getStatusBuffer() {
		return statusBuffer;
	}

	public void setStatusBuffer(Buffer statusBuffer) {
		this.statusBuffer = statusBuffer;
	}

	public int getId() {
		return networkId;
	}
	
	public BufferCollection getBuffers() {
		return buffers;
	}


	public void addBuffer(Buffer buffer) {
		buffers.addBuffer(buffer);
		buffer.addObserver(this);
	}


	public void setName(String networkName) {
		this.networkName = networkName;
		if(statusBuffer != null) //TODO: remember to handle when adding status buffer after connect
			statusBuffer.getInfo().name = networkName;
	}


	public String getName() {
		return networkName;
	}


	public void setConnected(Boolean isConnected) {
		this.isConnected = isConnected;
		this.open = isConnected;
	}


	public Boolean isConnected() {
		return isConnected;
	}


	public void setUserList(List<IrcUser> userList) {
		this.userList = userList;
	}


	public List<IrcUser> getUserList() {
		return userList;
	}


	@Override
	public void update(Observable observable, Object data) {
		setChanged();
		notifyObservers();
		
	}


	@Override
	public int compareTo(Network another) {
		return getId() - another.getId();
	}


	public void setOpen(boolean open) {
		this.open = open;
	}


	public boolean isOpen() {
		return open;
	}


	public String getNick() {
		return nick;
	}


	public void setNick(String nick) {
		this.nick = nick;
	}
	
	public void onUserJoined(IrcUser user) {
		userList.add(user);
	}


	public void onUserQuit(String nick) {
		for(IrcUser user: userList) {
			if(user.nick.equals(nick)) {
				for(Buffer buffer : buffers.getRawBufferList()) {
					if(user.channels.contains(buffer.getInfo().name)) {
						buffer.removeNick(nick);
					}
				}
				userList.remove(user);
				return;
			}
		}
	}


	public void onUserParted(String nick, String bufferName) {
		for(IrcUser user: userList) {
			if(user.nick.equals(nick) && user.channels.contains(bufferName)) {
				user.channels.remove(bufferName);
				break;
			}
		}
		for(Buffer buffer : buffers.getRawBufferList()) {
			if(buffer.getInfo().name.equals(bufferName)) {
				buffer.removeNick(nick);
				break;
			}
		}
	}


	public boolean hasNick(String nick) {
		for(IrcUser user : userList) {
			if(user.nick.equals(nick)) {
				return true;
			}
		}
		return false;
	}


	public IrcUser getUserByNick(String nick) {
		for(IrcUser user : userList) {
			System.out.println(user.nick);
			if(user.nick.equals(nick)) {
				return user;
			}
		}
		return null;
	}

	public boolean containsBuffer(int id) {
		return buffers.hasBuffer(id);
	}


	public int getBufferCount() {
		return buffers.getBufferCount();
	}
}
