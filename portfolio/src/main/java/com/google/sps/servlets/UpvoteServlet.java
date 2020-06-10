package com.google.sps.servlets;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/upvote-data")
public class UpvoteServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(UpvoteServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    // make sure all required parameters are present
    if(request.getParameter("id") != null) {
      long id = Long.parseLong(request.getParameter("id"));
      int vote = 0;
      if(request.getParameter("vote") != null) {
        vote = Integer.parseInt(request.getParameter("vote"));
      } else {
        LOGGER.log(Level.WARNING, "no vote provided");
        return;
      }
      UserService userService = UserServiceFactory.getUserService();
      if(!userService.isUserLoggedIn()) {
        LOGGER.log(Level.WARNING, "User not currently logged in, returning.");
        return;
      }
      String email = userService.getCurrentUser().getEmail();
      String[] temp = email.split("@");
      String email2 = temp[0] + "%40" + temp[1];
      Key key = KeyFactory.createKey("Comment", id);
      int statusCode = vote;
      try {
        Entity comment = datastore.get(key);
        int upvotes = Integer.parseInt(comment.getProperty("upvotes").toString());
        // check if user has already voted on this comment
        if(comment.hasProperty(email)) {
          int currentVote = Integer.parseInt(comment.getProperty(email).toString());
          if(vote != currentVote) {
            // reverse the vote
            upvotes -= (currentVote - vote);
            comment.setProperty(email, vote);
          } else {
            // unselect the vote
            comment.removeProperty(email);
            upvotes -= vote;
            statusCode = 0;
          }
        } else {
          upvotes += vote;
          comment.setProperty(email, vote);
        }
        comment.setProperty("upvotes", upvotes);
        datastore.put(comment);
        LOGGER.info("changed upvote count for comment id " + id);

        response.setContentType("text/html;");
        response.getWriter().println(statusCode);
      } catch (EntityNotFoundException e) {
        response.setContentType("text/html;");
        response.getWriter().println("comment not found");
        LOGGER.log(Level.WARNING, "comment not found for id " + id);
      }
    } else {
      LOGGER.info("no comment id provided");
      return;
    }
  }
}
