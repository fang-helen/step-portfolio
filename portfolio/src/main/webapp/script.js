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
var sort = "";
var numElems = 10;
var pg = 1;

/**
 * Adjusts layout of page at load time based on window size.
 * If the window is narrow, displayes items in a single column instead of side-by-side
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

/* fetches conmment data from servlet */
async function getComments() {
  const limit = document.getElementById("limit").value;
  const direction = document.getElementById("sort-dir").value;
  const response = await fetch("/data?limit=" + limit + "&sort=" + direction);
  js = await response.json();

  refreshComments();
}

function pageUp() {
    if(pg < Math.ceil(js.length/numElems)) {
        pg ++;
        refreshComments();
    }
}

function pageDown() {
    if(pg > 1) {
        pg --;
        refreshComments();
    }
}

/* refreshes the comment display div based on updated settings */
function refreshComments() {
  const target = document.getElementById("comment-list");
  target.textContent = "";
  for(var i = (pg-1)*numElems; i < js.length && i < pg*numElems; i++) {
    target.appendChild(createElement(js[i].propertyMap.content, js[i].propertyMap.timestamp));
  }
  const pageCount = document.getElementById("page-count");
  pageCount.innerHTML = pg + "/" + Math.ceil(js.length/numElems);
}

/* creates a <p> element and returns it */
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

/* condensed toString of Date information */
function dateString(date) {
    const year = date.getFullYear();
    const month = months[date.getMonth()];
    const day = date.getDate();
    const hour = date.getHours();
    const min = date.getMinutes();
    return hour + ":" + min + "   " + month + " " + day + ", " + year;
}

/* comparator function to list newest first */
function compareNewest(a, b) {
    return b.propertyMap.timestamp - a.propertyMap.timestamp;
}

/* comparator function to list oldest first */
function compareOldest(a, b) {
    return a.propertyMap.timestamp - b.propertyMap.timestamp;
}
