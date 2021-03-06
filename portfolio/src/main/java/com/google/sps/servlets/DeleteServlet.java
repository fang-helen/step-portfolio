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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@WebServlet("/delete-data")
public class DeleteServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(DeleteServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    if(request.getParameter("id") != null) {
      long id = Long.parseLong(request.getParameter("id"));
      Key key = KeyFactory.createKey("Comment", id);
      datastore.delete(key);
      LOGGER.info("deleted comment with id " + id);
    } else {
      LOGGER.info("id parameter is null, do nothing");
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("Comment");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    for(Entity entity:results.asIterable()) {
      datastore.delete(entity.getKey());
    }

    LOGGER.info("deleted all comments from the database");

    response.sendRedirect("/index.html");
  }
}
