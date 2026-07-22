package com.LinkaProject.linkaLite;
public class Comment {
    private int commentId;
    private int postId;
    private String username;
    private String textComment;

    public Comment(int commentId, int postId, String username, String textComment) {
        this.commentId = commentId;
        this.postId = postId;
        this.username = username;
        this.textComment = textComment;
    }

    public int getCommentId() { return commentId; }
    public int getPostId() { return postId; }
    public String getUsername() { return username; }
    public String getTextComment() { return textComment; }
}