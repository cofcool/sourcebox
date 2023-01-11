
package net.cofcool.toolbox.internal.trello;


import com.google.gson.annotations.SerializedName;


@SuppressWarnings("unused")
public class CheckItemState {

    @SerializedName("idCheckItem")
    private String mIdCheckItem;
    @SerializedName("state")
    private String mState;

    public String getIdCheckItem() {
        return mIdCheckItem;
    }

    public void setIdCheckItem(String idCheckItem) {
        mIdCheckItem = idCheckItem;
    }

    public String getState() {
        return mState;
    }

    public void setState(String state) {
        mState = state;
    }

}
