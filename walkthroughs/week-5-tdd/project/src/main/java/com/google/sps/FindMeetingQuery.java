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

package com.google.sps;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // list of free TimeRanges available in the day
    List<TimeRange> partition = new ArrayList<>();
    Collection<String> attendees = request.getAttendees();
    partition.add(TimeRange.fromStartDuration(TimeRange.START_OF_DAY, TimeRange.WHOLE_DAY));

    // remove all conflicting events from the list of free TimeRanges
    for(Event e: events) {
      Collection<String> eventAttendees = e.getAttendees();

      if(hasOverlap(attendees, eventAttendees)) {
        TimeRange when = e.getWhen();
        // temp list used for re-partitioning
        List<TimeRange> temp = new ArrayList<>();
        int firstAffected = -1;
        int lastAffected = -1;
        for(int i = 0; i < partition.size() && firstAffected < 0; i ++) {
          if(partition.get(i).overlaps(when)) {
            firstAffected = i;
          } else {
            temp.add(partition.get(i));
          }
        }
        // make changes only if free times have been affected
        if(firstAffected >= 0) {
          for(int i = firstAffected + 1; i < partition.size() && lastAffected < 0; i ++) {
            if(!partition.get(i).overlaps(when)) {
              lastAffected = i - 1;
            }
          }
          List<TimeRange> afterSplice = splice(partition, firstAffected, lastAffected, when);
          for(TimeRange t: afterSplice) {
            temp.add(t);
          }
          for(int i = lastAffected + 1; i < partition.size(); i ++) {
            temp.add(partition.get(i));
          }
        }
        partition = temp;
      }
    }

    return partition;
  }

  private int eventStart(Event e) {
    return e.when().start();
  }

  private int eventEnd(Event e) {
    return eventStart(e) + e.when().duration();
  }

  // checks if an event would affect the ability of any requested attendees to attend the meeting 
  private boolean hasOverlap(Collection<String> requestedAttendees, Collection<String> eventAttendees) {
    for(String req: requestedAttendees) {
      if(eventAttendees.contains(req)) {
        return true;
      }
    }
    return false;
  }

  private List<TimeRange> splice(List<TimeRange> partition, int first, int last, TimeRange when) {
    TimeRange firstRange = partition.get(first);
    TimeRange lastRange = partition.get(last);
    List<TimeRange> result = new ArrayList<>();
    // case 1: when spans OVER first and last -> result.size() == 0

    // case 2: when starts before first, ends during last -> result.size() == 1

    // case 3: when starts during first, ends after last -> result.size() == 1

    // case 4: when starts during first and ends during last -> result.size() == 2

    return result;
  }
}
