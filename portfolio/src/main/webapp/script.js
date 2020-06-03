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

const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"];
var js = "";

// current sort direction
var sortDir = "descending";
// num comments per pages
var numElemsPerPage = 5;
// total comments shown on page
var totalElems = 15;
// current page number
var pg = 1;


/**
 * Adjusts layout of page at load time based on window size.
 * If the window is narrow, displayes items in a single column instead of side-by-side.
 */
function loadLayout() {
  if (window.innerWidth < 1000) {
    var body = document.getElementById("body");
    body.style.display = "block";

    var left = document.getElementById("content-left");
    left.style.margin = "auto";
    left.style.padding = "0";

    var right = document.getElementById("content-right");
    right.style.border = "none";
    right.style.margin = "auto";
    right.style.padding = "0";
  }

  document.getElementById("sort-dir").value = sortDir;
  document.getElementById("limit").value = totalElems;
  document.getElementById("pg-limit").value = numElemsPerPage;
}

/**
 * Adds a random fact to the page.
 */
function addRandomGreeting() {
  const greetings = [
      'I speak fluent Mandarin and some Spanish.',
      'I really like Chinese dramas.',
      'I\'ve sewn a lot of pencil bags, but don\'t use most of them.',
      'I have a younger brother at home. We\'re 9 years apart.',
      'I think pineapple on pizza is not bad.',
      'I might double major in math.',
      'I was born in Utah and spent 5 years in Oregon before moving to Texas.',
    ];

  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}

/**
 * Applies a rotation effect to the clicked icon and toggles the drop-down.
 * If the icon is a "+", it is rotated into an "x" and the drop-down contents are displayed.
 * If the icon is an "x", it is rotated back into a "+" and the drop-down is collapsed.
 */
function rotateItem(index) {
  var icon = document.getElementsByClassName("plus")[index];
  var text = document.getElementsByClassName("dropdown-text")[index];
  var textContent = document.getElementsByClassName("dropdown-text-content")[index];
  if(!icon.classList.contains("clicked")) {
    icon.classList.add("clicked");
    text.style.height = (textContent.clientHeight + 60) + "px";
  } else {
    icon.classList.remove("clicked");
    text.style.height = "0";
  }
}

/* Fetches comment data from the servlet and refreshes the comment box content. */
async function getAndRefreshComments() {
  const response = await fetch("/data?limit=" + totalElems + "&sort=" + sortDir);
  js = await response.json();

  refreshComments();
}

/* Updates the comment display based on user input. */
function commentConfig() {
  var newSort = document.getElementById("sort-dir").value;
  var newElemsPerPage = parseInt(document.getElementById("pg-limit").value);
  var newTotal = parseInt(document.getElementById("limit").value);
  var needGet = false;
  var needRefresh = false;

  if(newSort.localeCompare(sortDir) != 0 || newTotal > js.length) {
    needGet = true;
  } else if (newTotal < totalElems || numElemsPerPage != newElemsPerPage) {
    needRefresh = true;
  }

  sortDir = newSort;
  totalElems = newTotal;
  numElemsPerPage = newElemsPerPage;

  if(needGet) {
    getAndRefreshComments();
  } else if (needRefresh) {
    refreshComments();
  }
}
 
/* Increments the comment page number. */
function pageUp() {
  if(pg < computeMaxPage()) {
    pg ++;
    refreshComments();
  }
}

/* Decrements the comment page number. */
function pageDown() {
  if(pg > 1) {
    pg --;
    refreshComments();
  }
}

/* Refreshes the comment display div using the corresponding global variables. */
function refreshComments() {
  var maxPage = computeMaxPage();
  if(pg > maxPage) {
    pg = maxPage;
  } else if (pg < 1) {
    pg = 1;
  }

  const target = document.getElementById("comment-list");
  target.textContent = "";
  for(var i = (pg-1)*numElemsPerPage; i < Math.min(js.length, totalElems) && i < pg*numElemsPerPage; i++) {
    target.appendChild(createElement(js[i].propertyMap.content, js[i].propertyMap.timestamp));
  }
  const pageCount = document.getElementById("page-count");
  pageCount.innerHTML = pg + "/" + maxPage;

  var icon = document.getElementById("plus-comment");
  var text = document.getElementById("comment-wrapper");
  var textContent = document.getElementById("comment-box");
  if(icon.classList.contains("clicked")) {
    text.style.height = (textContent.clientHeight + 60) + "px";
  }
}

/* Computes and returns the maximum page number. */
function computeMaxPage() {
  return Math.ceil(Math.min(js.length, totalElems)/numElemsPerPage);
}

/** 
 * Creates a <div> element for an individual comment and returns it. 
 * 
 * @param {string} text The text content of the comment.
 * @param {number} millis The timestamp, in milliseconds, of the comment.
 */
function createElement(text, millis) {
  const wrapper = document.createElement("div");
  wrapper.className = "comment";
  const textWrapper = document.createElement("div");
  textWrapper.className = "comment-text";
  const timeWrapper = document.createElement("div");
  timeWrapper.className = "comment-time";

  const date = new Date(millis);
  textWrapper.innerText = text;
  timeWrapper.innerText = dateString(date);
  wrapper.appendChild(textWrapper);
  wrapper.appendChild(timeWrapper);
  return wrapper;
}

/* Returns a condensed toString of Date information. */
function dateString(date) {
  const year = date.getFullYear();
  const month = months[date.getMonth()];
  const day = date.getDate();
  const hour = date.getHours();
  const min = date.getMinutes();
  return hour + ":" + min + "   " + month + " " + day + ", " + year;
}

/* Deletes all comments from the database and refreshes the page. */
async function deleteAllComments() {
    await fetch(new Request("/delete-data", {method: "POST"}));
    getAndRefreshComments();
}
