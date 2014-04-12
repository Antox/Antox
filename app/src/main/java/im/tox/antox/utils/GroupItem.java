package im.tox.antox.utils;

/**
 * Created by dragos on 10.04.2014.
 */
public class GroupItem {
    private String groupName;
    private int numberOfMembers;

    public GroupItem() {
        super();
    }

    public GroupItem(String groupName) {
        this.groupName = groupName;
        numberOfMembers = 0;
    }

    public GroupItem(String groupName, int numberOfMembers) {
        this.groupName = groupName;
        this.numberOfMembers = numberOfMembers;
    }

    public int getNumberOfMembers() {
        return numberOfMembers;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setNumberOfMembers(int numberOfMembers) {
        this.numberOfMembers = numberOfMembers;
    }
}
