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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    Collection<Event> optionalAttendeeEvents = new ArrayList<>();
    Map<String, Schedule> mandatorySchedules = new HashMap<>();
    Map<String, Schedule> optionalSchedules = new HashMap<>();
    for(Event e: events) {
      // build mandatory attendee schedules
      Collection<String> eventAttendees = e.getAttendees();
      List<String> mandatoryOverlap = attendanceOverlap(attendees, eventAttendees);
      for(String name: mandatoryOverlap) {
        Schedule s = mandatorySchedules.get(name);
        if(s == null) {
          s = new Schedule();
          mandatorySchedules.put(name, s);
        }
        s.schedule.add(e.getWhen());
      }
      // build optional attendee schedules
      List<String> optionalOverlap = attendanceOverlap(optionalAttendees, eventAttendees);
      for(String name: optionalOverlap) {
        Schedule s = optionalSchedules.get(name);
        if(s == null) {
          s = new Schedule();
          optionalSchedules.put(name, s);
        }
        s.schedule.add(e.getWhen());
      }
      if(mandatoryOverlap.size() > 0) {
        mandatoryAttendeeEvents.add(e);
      }
      if(optionalOverlap.size() > 0) {
        optionalAttendeeEvents.add(e);
      }
    }

    // query for mandatory attendees first
    List<TimeRange> mandatory = performQuery(mandatoryAttendeeEvents, duration, attendees, partition);

    // check if any of these slots work for the optional attendees
    List<TimeRange> withOptional = performQuery(optionalAttendeeEvents, duration, optionalAttendees, mandatory);

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
   * @param attendees An attendee list to attempt to accommodate for.
   * @return          A collection of suitable TimeRanges that will not create time 
   *                  conflicts for any requested attendees.
   */
  private List<TimeRange> performQuery(
      Collection<Event> events, 
      long duration, 
      Collection<String> attendees, 
      List<TimeRange> partition) 
  {
    for(Event e: events) {
      Collection<String> eventAttendees = e.getAttendees();
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

  private List<TimeRange> performBestFitQuery(
      Collection<Event> events, 
      long duration, 
      Collection<String> attendees, 
      List<TimeRange> partition,
      Map<String, Schedule> schedules) 
  {
    for(Event e: events) {
      Collection<String> eventAttendees = e.getAttendees();
      TimeRange when = e.getWhen();        
      // create temp partition and refresh the partition
      partition = repartitionTimeRanges(partition, when);
    }
    // check to see which free timeslots are long enough
    List<TimeRange> freeTimes = new ArrayList<>();
    List<Integer> bestTimes = new ArrayList<>();
    int mostAttendees = 0;
    for(int i = 0; i < partition.size(); i ++) {
      TimeRange t = partition.get(i);
      if(t.duration() >= duration) {
        int count = 0;
        for(String name: schedules.keySet()) {
          if(!Schedule.overlapsSchedule(schedules.get(name), t)) {
            count ++;
          }
        }
        if(count == mostAttendees) {
          bestTimes.add(i);
        } else if (count > mostAttendees) {
          bestTimes = new ArrayList<>();
          mostAttendees = count;
          bestTimes.add(i);
        }
      }
    }
    for(int index: bestTimes) {
      freeTimes.add(partition.get(index));
    }
    return freeTimes;
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

  private static class Schedule {
    
    private List<TimeRange> schedule;

    private Schedule() {
      schedule = new ArrayList<>();
    }

    private static boolean overlapsSchedule(Schedule s, TimeRange when) {
      for(TimeRange t: s.schedule) {
        if(t.overlaps(when)) {
          return true;
        }
      }
      return false;
    }
  }
}
