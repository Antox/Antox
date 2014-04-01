package im.tox.antox.utils;

import java.util.ArrayList;

public class AntoxState {
	public static final int NO_CHAT_PARTNER = -1;
	private static AntoxState instance;

	private ArrayList<String> boundActivities;
	private int activeChatPartner;

	private AntoxState() {
		this.boundActivities = new ArrayList<String>();
		this.activeChatPartner = NO_CHAT_PARTNER;
	}

	public static AntoxState getInstance() {
        /* Double-checked locking */
		if (instance == null) {
            synchronized (AntoxState.class) {
			    if (instance == null) {
                    instance = new AntoxState();
                }
            }
		}
		return instance;
	}

	/**
	 * Get the list of currently bound activities. This needs to be a List,
	 * because onResume() in a new Activity might be called before onPause() in
	 * the old Activity.
	 * 
	 * @return the activities currently bound to the ToxService
	 */
	public ArrayList<String> getBoundActivities() {
		return this.boundActivities;
	}

	/**
	 * Get the friendnumber of the currently active Chat Partner
	 * 
	 * @return friendnumber of the currently active Chat Partner.
	 *         {@link AntoxState#NO_CHAT_PARTNER} if no chat partner is
	 *         currently active
	 */
	public int getActiveChatPartner() {
		return this.activeChatPartner;
	}

	/**
	 * Set the currently active Chat partner
	 * 
	 * @param partner
	 *            chat partner friendnumber to set
	 */
	public void setActiveChatPartner(int partner) {
		this.activeChatPartner = partner;
	}
}
