package im.tox.antox.utils;

import im.tox.jtoxcore.FriendExistsException;
import im.tox.jtoxcore.FriendList;
import im.tox.jtoxcore.ToxFriend;
import im.tox.jtoxcore.ToxUserStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Wrapper class for the Tox Friendlist.
 * 
 * @author sonOfRa
 * 
 */
public class AntoxFriendList implements FriendList<AntoxFriend> {

	/**
	 * Underlying friend list
	 */
	private List<AntoxFriend> friends;

	/**
	 * Create a new, empty ToxFriendList instance
	 */
	public AntoxFriendList() {
		this.friends = Collections
				.synchronizedList(new ArrayList<AntoxFriend>());
	}

	/**
	 * Create a new ToxFriendList instance with the given List of Friends as an
	 * underlying structure
	 * 
	 * @param friends
	 *            initial list of friends to use
	 */
	public AntoxFriendList(ArrayList<AntoxFriend> friends) {
		this.friends = friends;
	}

	@Override
	public AntoxFriend getByFriendNumber(int friendnumber) {
		synchronized (this.friends) {
			for (AntoxFriend friend : this.friends) {
				if (friend.getFriendnumber() == friendnumber) {
					return friend;
				}
			}
		}
		return null;
	}

	@Override
	public AntoxFriend getById(String id) {
        if(id!=null) {
            synchronized (this.friends) {
                for (AntoxFriend friend : this.friends) {
                    if (id.equals(friend.getId())) {
                        return friend;
                    }
                }
            }
        }
		return null;
	}

	@Override
	public List<AntoxFriend> getByName(String name, boolean ignorecase) {
		if (ignorecase) {
			return getByNameIgnoreCase(name);
		}

		ArrayList<AntoxFriend> result = new ArrayList<AntoxFriend>();
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
				if (name == null && f.getName() == null) {
					result.add(f);
				} else if (name != null && name.equals(f.getName())) {
					result.add(f);
				}
			}
		}
		return result;
	}

	private List<AntoxFriend> getByNameIgnoreCase(String name) {
		ArrayList<AntoxFriend> result = new ArrayList<AntoxFriend>();
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
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
	public List<AntoxFriend> searchFriend(String partial) {
		if (partial == null) {
			throw new IllegalArgumentException("Cannot search for null");
		}
		String partialLowered = partial.toLowerCase(Locale.US);
		ArrayList<AntoxFriend> result = new ArrayList<AntoxFriend>();
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
				String name = (f.getName() == null) ? null : f.getName()
						.toLowerCase(Locale.US);
				if (name.contains(partialLowered)) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<AntoxFriend> getByStatus(ToxUserStatus status) {
		ArrayList<AntoxFriend> result = new ArrayList<AntoxFriend>();
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
				if (f.isOnline() && f.getStatus() == status) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<AntoxFriend> getOnlineFriends() {
		ArrayList<AntoxFriend> result = new ArrayList<AntoxFriend>();
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
				if (f.isOnline()) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<AntoxFriend> getOfflineFriends() {
		ArrayList<AntoxFriend> result = new ArrayList<AntoxFriend>();
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
				if (!f.isOnline()) {
					result.add(f);
				}
			}
		}
		return result;
	}

	@Override
	public List<AntoxFriend> all() {
		return new ArrayList<AntoxFriend>(this.friends);
	}

	@Override
	public AntoxFriend addFriend(int friendnumber) throws FriendExistsException {
		AntoxFriend friend;
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
				if (f.getFriendnumber() == friendnumber) {
					throw new FriendExistsException(f.getFriendnumber());
				}
			}
			friend = new AntoxFriend(friendnumber);
			this.friends.add(friend);
		}
		return friend;
	}

	@Override
	public AntoxFriend addFriendIfNotExists(int friendnumber) {
		synchronized (this.friends) {
			for (AntoxFriend f : this.friends) {
				if (f.getFriendnumber() == friendnumber) {
					return f;
				}
			}
			AntoxFriend friend = new AntoxFriend(friendnumber);
			this.friends.add(friend);
			return friend;
		}
	}

	@Override
	public void removeFriend(int friendnumber) {
		synchronized (this.friends) {
			Iterator<AntoxFriend> it = this.friends.iterator();
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