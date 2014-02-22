package im.tox.antox;

import im.tox.jtoxcore.FriendExistsException;
import im.tox.jtoxcore.FriendList;
import im.tox.jtoxcore.ToxFriend;
import im.tox.jtoxcore.ToxUserStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper class for the Tox Friendlist.
 * 
 * @author sonOfRa
 * 
 */
public class ToxFriendList implements FriendList<ToxFriend> {

	/**
	 * Underlying friend list
	 */
	private List<ToxFriend> friends;

	/**
	 * Create a new, empty ToxFriendList instance
	 */
	public ToxFriendList() {
		this.friends = Collections.synchronizedList(new ArrayList<ToxFriend>());
	}

	/**
	 * Create a new ToxFriendList instance with the given List of Friends as an
	 * underlying structure
	 * 
	 * @param friends
	 *            initial list of friends to use
	 */
	public ToxFriendList(ArrayList<ToxFriend> friends) {
		this.friends = friends;
	}

	@Override
	public ToxFriend getByFriendNumber(int friendnumber) {
		synchronized (this.friends) {
			for (ToxFriend friend : this.friends) {
				if (friend.getFriendnumber() == friendnumber) {
					return friend;
				}
			}
		}
		return null;
	}

	@Override
	public ToxFriend getById(String id) {
		synchronized (this.friends) {
			for (ToxFriend friend : this.friends) {
				if (id == friend.getId()) {
					return friend;
				}

				if (id.equals(friend.getId())) {
					return friend;
				}
			}
		}
		return null;
	}

	@Override
	public List<ToxFriend> getByName(String name, boolean ignorecase) {
		if (ignorecase) {
			return getByNameIgnoreCase(name);
		}

		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (name == null && f.getName() == null) {
					result.add(f);
				} else if (name != null && name.equals(f.getName())) {
					result.add(f);
				}
			}
		}
		return result;
	}

	private List<ToxFriend> getByNameIgnoreCase(String name) {
		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (name == null && f.getName() == null) {
					result.add(f);
				} else if (name != null && name.equalsIgnoreCase(f.getName())) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<ToxFriend> getByNickname(String nickname, boolean ignorecase) {
		if (ignorecase) {
			return getByNicknameIgnoreCase(nickname);
		}

		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (nickname == null && f.getNickname() == null) {
					result.add(f);
				} else if (nickname != null && nickname.equals(f.getName())) {
					result.add(f);
				}
			}
		}
		return result;
	}

	private List<ToxFriend> getByNicknameIgnoreCase(String nickname) {
		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (nickname == null && f.getNickname() == null) {
					result.add(f);
				} else if (nickname != null
						&& nickname.equalsIgnoreCase(f.getName())) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<ToxFriend> searchFriend(String partial) {
		if (partial == null) {
			throw new IllegalArgumentException("Cannot search for null");
		}
		String partialLowered = partial.toLowerCase();
		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				String name = f.getName() == null ? null : f.getName()
						.toLowerCase();
				String nick = f.getNickname() == null ? null : f.getNickname()
						.toLowerCase();
				if (name.contains(partialLowered)
						|| nick.contains(partialLowered)) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<ToxFriend> getByStatus(ToxUserStatus status) {
		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (f.isOnline() && f.getStatus() == status) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<ToxFriend> getOnlineFriends() {
		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (f.isOnline()) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<ToxFriend> getOfflineFriends() {
		ArrayList<ToxFriend> result = new ArrayList<ToxFriend>();
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (!f.isOnline()) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<ToxFriend> all() {
		return new ArrayList<ToxFriend>(this.friends);
	}

	@Override
	public ToxFriend addFriend(int friendnumber) throws FriendExistsException {
		ToxFriend friend;
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (f.getFriendnumber() == friendnumber) {
					throw new FriendExistsException(f.getFriendnumber());
				}
			}
			friend = new ToxFriend(friendnumber);
			this.friends.add(friend);
		}
		return friend;
	}

	@Override
	public ToxFriend addFriendIfNotExists(int friendnumber) {
		synchronized (this.friends) {
			for (ToxFriend f : this.friends) {
				if (f.getFriendnumber() == friendnumber) {
					return f;
				}
			}
			ToxFriend friend = new ToxFriend(friendnumber);
			this.friends.add(friend);
			return friend;
		}
	}

	@Override
	public void removeFriend(int friendnumber) {
		synchronized (this.friends) {
			Iterator<ToxFriend> it = this.friends.iterator();
			while (it.hasNext()) {
				ToxFriend f = it.next();
				if (f.getFriendnumber() == friendnumber) {
					it.remove();
					break;
				}
			}
		}
	}

}