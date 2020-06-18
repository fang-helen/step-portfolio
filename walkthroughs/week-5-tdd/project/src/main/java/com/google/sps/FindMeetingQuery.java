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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    partition.add(TimeRange.WHOLE_DAY);
    long duration = request.getDuration();
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();

    Collection<Event> mandatoryAttendeeEvents = new ArrayList<>();
    Map<String, List<Event>> optionalSchedules = new HashMap<>();
    for(Event e: events) {
      // build mandatory attendee schedules
      Collection<String> eventAttendees = e.getAttendees();
      if(attendanceOverlap(attendees, eventAttendees).size() > 0) {
        mandatoryAttendeeEvents.add(e);
      }
      // build optional attendee schedules
      List<String> optionalOverlap = attendanceOverlap(optionalAttendees, eventAttendees);
      for(String name: optionalOverlap) {
        List<Event> s = optionalSchedules.get(name);
        if(s == null) {
          s = new ArrayList<>();
          optionalSchedules.put(name, s);
        }
        s.add(e);
      }
    }
    // query for mandatory attendees first
    List<TimeRange> mandatory = findSlots(mandatoryAttendeeEvents, duration, partition);
    if(optionalAttendees.size() <= 0) {
      return mandatory;
    }
    // check if any of these slots work for any optional attendees
    Collection<TimeRange> withOptional = findSlotsWithMostAttendees(duration, mandatory, optionalSchedules);

    if(attendees.size() <= 0) {
      // no mandatory attendees to worry about, decide based on optional attendee list
      return withOptional;
    } else if(withOptional.size() <= 0) {
      // no non-conflicting times for optional attendees, so mandatory takes higher priority
      return mandatory;
    }
    return withOptional;
  }

  /**
   * Finds non-conflicting timeslots to schedule a requested meeting based on existing partition.
   * @param events    A Collection of existing events that must be accounted for when
   *                  searching for a suitable time slot.
   * @param duration  The duration of the requested meeting.
   * @param partition The existing timeslot partition to base off of.
   * @return          A collection of suitable TimeRanges that will not create time 
   *                  conflicts for any requested attendees.
   */
  private List<TimeRange> findSlots(
      Collection<Event> events, 
      long duration, 
      List<TimeRange> partition) 
  {
    for(Event e: events) {
      TimeRange when = e.getWhen();        
      // create temp partition and refresh the partition
      partition = repartitionTimeRanges(partition, when);
    }
    // check to see which free timeslots are long enough
    List<TimeRange> freeTimes = new ArrayList<>();
    for(TimeRange t: partition) {
      if(t.duration() >= duration) {
        freeTimes.add(t);
      }
    }
    return freeTimes;
  }

  /**
   * Finds optimal timeslots to schedule a requested meeting based on an existing
   * partition, returning timeslots with the highest attendee availability.
   * @param duration  The duration of the requested meeting.
   * @param partition The existing timeslot partition to base off of.
   * @param schedules A map of attendees to their existing event commitments to schedule around.
   * @return          A collection of TimeRanges that will allow the greatest number of attendees
   *                  to participate. If multiple timeslots allow the same number of participants,
   *                  they will all be returned.
   */
  private Collection<TimeRange> findSlotsWithMostAttendees(
      long duration, 
      List<TimeRange> partition,
      Map<String, List<Event>> schedules) 
  {
    // remove optional attendees that cannot make any of the slots to reduce sample size
    Map<String, List<Event>> updatedSchedules = new HashMap<>();
    List<String> nameList = new ArrayList<>();
    for(String name: schedules.keySet()) {
      if(findSlots(schedules.get(name), duration, partition).size() > 0) {
        updatedSchedules.put(name, schedules.get(name));
        nameList.add(name);
      }
    }
    if(nameList.size() <= 0) {
      return partition;
    }
    // perform recursive partition-building
    Collection<TimeRange> result = recursiveQuery(duration, partition, updatedSchedules, nameList, 0, 0, new int[1]);
    return result;
  }

  /**
   * A recursive helper method for schedule-optimizing.
   * @param duration  The duration of the requested meeting.
   * @param partition The existing timeslot partition to base off of.
   * @param schedules A map of attendees to their existing event commitments to schedule around.
   * @param nameList  An indexed list of keys for the schedules map.
   * @param index     The position in nameList for the current recursive call.
   * @param depth     The depth of the current recursive call, or the number of attendees
   *                  that the current partition accommodates for.
   * @param maxDepth  Container to hold the maximum depth achieved by the current partition on return,
   *                  in order to return multiple items.    
   * @return          A collection of TimeRanges that will allow the greatest number of attendees
   *                  to participate. If multiple timeslots allow the same number of participants,
   *                  they will all be returned.
   */
  private Collection<TimeRange> recursiveQuery(long duration, List<TimeRange> partition, 
        Map<String, List<Event>> schedules, List<String> nameList, int index, int depth,
        int[] maxDepth) 
  {
    maxDepth[0] = depth;
    // base case: reached end of list, no more attendees to check
    if(index == nameList.size()) {
      return partition;
    }
    // recursive case. keep the TimeRanges that will yield maximum depth (more attendees)
    Collection<TimeRange> result = new TreeSet<>(TimeRange.ORDER_BY_START);
    for(int i = index; i < nameList.size(); i ++) {
      String currentName = nameList.get(i);
      List<TimeRange> intermediatePartition 
            = findSlots(schedules.get(currentName), duration, partition);
      // keep going only if no conflicts for now
      if(intermediatePartition.size() > 0) {
        int[] getMaxDepth = new int[1];
        Collection<TimeRange> recursiveResult 
            = recursiveQuery(duration, intermediatePartition, schedules, 
                nameList, i + 1, depth + 1, getMaxDepth);
        if(getMaxDepth[0] > maxDepth[0]) {
          maxDepth[0] = getMaxDepth[0];
          result.clear();
        }
        if(getMaxDepth[0] >= maxDepth[0]) {
          for(TimeRange t: recursiveResult) {
            result.add(t);
          }
        }
      } 
    }
    // catch case where no non-conflicting partitions have been found
    if(result.size() == 0) {
      result = partition;
    }
    return result;
  }

  /** Checks for overlap between two attendee lists. */
  private List<String> attendanceOverlap(Collection<String> attendees1, Collection<String> attendees2) {
    List<String> result = new ArrayList<>();
    for(String req: attendees1) {
      if(attendees2.contains(req)) {
        result.add(req);
      }
    }
    return result;
  }

  /**
   * Creates a new, temporary partition to resolve conflicts between a TimeRange and and existing partition.
   * @param partition The current partition of TimeRanges.
   * @param when The TimeRange of interest to resolve conflicts with.
   * @return A new partition of TimeRanges that takes into account the conflicting TimeRange.
   */
  private List<TimeRange> repartitionTimeRanges(List<TimeRange> partition, TimeRange when) {
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
    return temp;
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
      temp = TimeRange.split(lastRange, when.end());
      result.add(temp.get(1));
    } else if(when.end() >= lastRange.end()) {
      // case 3: when starts during first, ends after last -> result.size() == 1
      temp = TimeRange.split(firstRange, when.start());
      result.add(temp.get(0));
    } else {
      // case 4: when starts during first and ends during last -> result.size() == 2
      temp = TimeRange.split(firstRange, when.start());
      result.add(temp.get(0));
      temp = TimeRange.split(lastRange, when.end());
      result.add(temp.get(1));
    }
    return result;
  }
}
