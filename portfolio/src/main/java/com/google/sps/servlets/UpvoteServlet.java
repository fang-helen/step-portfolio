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
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/upvote-data")
public class UpvoteServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(DataServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    if(request.getParameter("id") != null) {
      long id = Long.parseLong(request.getParameter("id"));
      int vote = 0;
      if(request.getParameter("vote") != null) {
        vote = Integer.parseInt(request.getParameter("vote"));
      }
      Key key = KeyFactory.createKey("Comment", id);
      try {
        Entity comment = datastore.get(key);
        comment.setProperty("upvotes", vote);
        datastore.put(comment);
        LOGGER.info("changed upvote count for comment id " + id);
      } catch (EntityNotFoundException e) {
        response.setContentType("text/html;");
        response.getWriter().println("comment not found");
        LOGGER.log(Level.WARNING, "comment not found for id " + id);
      }
    }
  }
}
