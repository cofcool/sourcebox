
package net.cofcool.toolbox.internal.trello;


import com.google.gson.annotations.SerializedName;


@SuppressWarnings("unused")
public class Label {

    @SerializedName("color")
    private String mColor;
    @SerializedName("id")
    private String mId;
    @SerializedName("idBoard")
    private String mIdBoard;
    @SerializedName("name")
    private String mName;

    public String getColor() {
        return mColor;
    }

    public void setColor(String color) {
        mColor = color;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getIdBoard() {
        return mIdBoard;
    }

    public void setIdBoard(String idBoard) {
        mIdBoard = idBoard;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

}
