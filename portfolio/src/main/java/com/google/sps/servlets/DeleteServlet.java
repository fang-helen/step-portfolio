package com.google.sps.servlets;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import java.util.ArrayList;
import java.util.List;

@WebServlet("/delete-data")
public class DeleteServlet extends HttpServlet {

  // used to temporarily hold datastore keys
  private List<Key> keys;
    
  @Override
  public void init() {
    keys = new ArrayList<>();
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("Comment");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    keys = new ArrayList<>();
    for(Entity entity:results.asIterable()) {
      keys.add(entity.getKey());
    }

    for(Key k: keys) {
        datastore.delete(k);
    }

    response.sendRedirect("/index.html");
  }
}
