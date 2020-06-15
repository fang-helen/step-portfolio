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

  /**
   * Finds non-conflicting timeslots to schedule a requested meeting.
   * @param events     A Collection of existing events that must be accounted for when
   *                  searching for a suitable time slot.
   * @param request   A MeetingRequest to be acommodated, including an attendee list 
   *                  and requested duration.
   * @return          A collection of suitable TimeRanges that will not create time 
   *                  conflicts for any requested attendees.
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // keep track of list of free TimeRanges available in the day
    List<TimeRange> partition = new ArrayList<>();

    if(request.getDuration() >= TimeRange.WHOLE_DAY.duration()) {
      return partition;
    }

    Collection<String> attendees = request.getAttendees();
    partition.add(TimeRange.WHOLE_DAY);

    // remove only conflicting timeslots from the list of free TimeRanges
    for(Event e: events) {
      Collection<String> eventAttendees = e.getAttendees();

      if(hasAttendanceOverlap(attendees, eventAttendees)) {
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
          // search for last affected timeslot
          for(int i = firstAffected + 1; i < partition.size() && lastAffected < 0; i ++) {
            if(!partition.get(i).overlaps(when)) {
              lastAffected = i - 1;
            }
          }
          if(lastAffected < 0) {
            lastAffected = partition.size() - 1;
          }
          // splice the conflicting times and add resulting free slots to new partition
          List<TimeRange> afterSplice = splice(partition, firstAffected, lastAffected, when);
          for(TimeRange t: afterSplice) {
            temp.add(t);
          }
          for(int i = lastAffected + 1; i < partition.size(); i ++) {
            temp.add(partition.get(i));
          }
        }
        // refresh the partition
        partition = temp;
      }
    }
    // check to see which free timeslots are long enough
    Collection<TimeRange> freeTimes = new ArrayList<>();
    for(TimeRange t: partition) {
      if(t.duration() >= request.getDuration()) {
        freeTimes.add(t);
      }
    }
    return freeTimes;
  }

  /** Checks for overlap between two attendee lists. */
  private boolean hasAttendanceOverlap(Collection<String> attendees1, Collection<String> attendees2) {
    for(String req: attendees1) {
      if(attendees2.contains(req)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Re-partitions an existing partition of free timeslots by removing all overlap with a given TimeRange.
   * @param partition The existing partition of free TimeRanges in the day.
   * @param first The index of the first TimeRange that will be affected in the original partition.
   * @param last The index of the first TimeRange taht will be affected in the original partition.
   * @param when The TimeRange of interest to remove from the existing partition.
   * @return A list containing timeslots in the modified partition for the range between first and last.
   */
  private List<TimeRange> splice(List<TimeRange> partition, int first, int last, TimeRange when) {
    TimeRange firstRange = partition.get(first);
    TimeRange lastRange = partition.get(last);
    List<TimeRange> result = new ArrayList<>();
    List<TimeRange> temp;
    if(when.start() <= firstRange.start() && when.end() >= lastRange.end()) {
      // case 1: when spans OVER first and last -> result.size() == 0
    } else if(when.start() <= firstRange.start()) {
      // case 2: when starts before first, ends during last -> result.size() == 1
      temp = split(lastRange, when.end());
      result.add(temp.get(1));
    } else if(when.end() >= lastRange.end()) {
      // case 3: when starts during first, ends after last -> result.size() == 1
      temp = split(firstRange, when.start());
      result.add(temp.get(0));
    } else {
      // case 4: when starts during first and ends during last -> result.size() == 2
      temp = split(firstRange, when.start());
      result.add(temp.get(0));
      temp = split(lastRange, when.end());
      result.add(temp.get(1));
    }
    return result;
  }

  /** Splits a TimeRange at a specified time into two TimeRanges and returns both as a list. */
  private List<TimeRange> split(TimeRange range, int time) {
    List<TimeRange> result = new ArrayList<>();
    int newDuration = time - range.start();
    result.add(TimeRange.fromStartDuration(range.start(), newDuration));
    result.add(TimeRange.fromStartDuration(time, range.duration() - newDuration));
    return result;
  }
}
