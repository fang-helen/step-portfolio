// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  // used for json conversion when extracting Entities from datastore query
  private List<Entity> database; 
  // upper limit of how many results to return
  private int limit; 
  // sort direction - ascending or descending
  private boolean descending; 
  // sort criteria
  private String sortParam; 

  @Override
  public void init() {
    database = new ArrayList<>();
    limit = 10;
    sortParam = "timestamp";
    descending = true;
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // querying datastore elements
    String paramLimit = request.getParameter("limit");
    String paramSort = request.getParameter("sort");
    String paramChoice = request.getParameter("sortBy");
    String auth = request.getParameter("auth");
    if(paramLimit != null) {
      limit = Integer.parseInt(paramLimit);
    }
    if(paramSort != null) {
      if(paramSort.equals("descending")) {
        descending = true;
      }
      else if (paramSort.equals("ascending")) {
        descending = false;
      } else {
        throw new IOException("invalid sort direction");
      }
    }
    if(paramChoice != null) {
      sortParam = paramChoice;
    }

    Query query = new Query("Comment");
    if(descending) {
      query.addSort(sortParam, SortDirection.DESCENDING);
    } else {
      query.addSort(sortParam, SortDirection.ASCENDING);
    }
    
    PreparedQuery results = datastore.prepare(query);

    database = new ArrayList<>();
    Iterator<Entity> itr = results.asIterable().iterator();
    if(auth != null && auth.length() > 0) {
      // add author filter to search
      int count = 0;
      while(count < limit && itr.hasNext()) {
        Entity entity = itr.next();
        String eAuth = entity.getProperty("author").toString();
        eAuth = eAuth.replaceAll("\\s", "");
        if(eAuth.equalsIgnoreCase(auth)) {
          count ++;
          database.add(entity);
        }
      }
    } else {
      for(int i = 0; i < limit && itr.hasNext(); i++) {
        database.add(itr.next());
      }
    }

    response.setContentType("application/json;");
    Gson gson = new Gson();
    String json = gson.toJson(database);
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = request.getParameter("enter-text");
    if(text == null || text.length() == 0) {
      response.sendRedirect("/index.html");
      return;
    }
    UserService userService = UserServiceFactory.getUserService();
    String auth = "";
    if(userService.isUserLoggedIn()) {
      auth = userService.getCurrentUser().getEmail();
    }

    Entity comment = new Entity("Comment");
    comment.setProperty("content", text);
    comment.setProperty("timestamp", System.currentTimeMillis());
    comment.setProperty("upvotes", 0);
    comment.setProperty("author", auth);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(comment);

    response.sendRedirect("/index.html");
  }
}
