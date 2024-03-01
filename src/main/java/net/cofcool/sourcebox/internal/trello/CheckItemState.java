
package net.cofcool.sourcebox.internal.trello;


import com.fasterxml.jackson.annotation.JsonProperty;


@SuppressWarnings("unused")
public class CheckItemState {

    @JsonProperty("idCheckItem")
    private String mIdCheckItem;
    @JsonProperty("state")
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
