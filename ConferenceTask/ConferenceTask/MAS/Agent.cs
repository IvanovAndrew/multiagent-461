using System;
using System.Linq;

namespace ConferenceTask.MAS
{
    public class Agent : ICommunication
    {
        public int Id;
        private readonly int[] _reportRatings;

        /// <summary>
        /// Minimal threshold
        /// </summary>
        private readonly int _minBound;
        
        /// <summary>
        /// First dimension is section number
        /// Second dimension is report rating
        /// Sorted by descending
        /// </summary>
        private readonly Report[][] _analisedSchedule;

        private Message _answer;

        public Agent(int id, int[] reportRatings)
        {
            Id = id;
            _reportRatings = reportRatings;

            _analisedSchedule = AnalysedScheduleInit();
            _minBound = CalculateBound();
        }

        #region initial methods
        private Report[][] AnalysedScheduleInit()
        {
            var res = new Report[Schedule.ReportsCountInSection][];
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                res[i] = new Report[Schedule.Sections];
            }
            return res;
        }

        /// <summary>
        /// Calculates the lowest threshold
        /// </summary>
        /// <returns></returns>
        private int CalculateBound()
        {
            var temp = _reportRatings.OrderByDescending(elem => elem).ToList();
            return temp[Schedule.ReportsCountInSection];
        }

        #endregion

        /// <summary>
        /// if agent likes current Schedule then returns true
        /// </summary>
        /// <returns></returns>
        public bool IsGoodShedule(Schedule schedule)
        {
            AnaliseShedule(schedule);
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                if (IsGoodTime(i)) continue;
                    return false;
            }
            return true;
        }

        /// <summary>
        /// Creates schedule which reports are sorted by descending on each rows
        /// </summary>
        /// <param name="schedule">current schedule</param>
        private void AnaliseShedule(Schedule schedule)
        {
            // fill
            foreach (Report report in schedule.Reports)
            {
                _analisedSchedule[report.PositionInSection][report.SectionNumber] = report;
            }

            // sorting
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                _analisedSchedule[i] = _analisedSchedule[i].OrderByDescending(elem => _reportRatings[elem.Id]).ToArray();
            }
        }
        
        /// <summary>
        /// Checks if maximal rating in the time is greather than minimal bound
        /// </summary>
        /// <param name="time"></param>
        /// <returns></returns>
        private bool IsGoodTime(int time)
        {
            var report = _analisedSchedule[time][0];
            return _reportRatings[report.Id] >= _minBound;
        }

        /// <summary>
        /// Returns best version of schedule in the opinion agent
        /// </summary>
        /// <param name="schedule">Current schedule</param>
        /// <returns></returns>
        public Schedule GetMyBestSchedule(Schedule schedule)
        {
            var newShedule = new Schedule();
            newShedule = schedule;
            for (int time = 0; time < Schedule.ReportsCountInSection; time++)
            {
                if (IsGoodTime(time)) continue;

                newShedule = ChangeShedule(time, newShedule);
            }
            return newShedule;
        }

        private int GetSecondTopIndex()
        {
            var max = -1;
            var index = -1;
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                var report = _analisedSchedule[i][1];
                if (_reportRatings[report.Id] > max)
                {
                    max = _reportRatings[report.Id];
                    index = i;
                }
            }
            return index;
        }

        /// <summary>
        /// Changed one report from "bad" time to "good" report 
        /// </summary>
        /// <param name="badTime"></param>
        /// <param name="schedule">planning Schedule</param>
        private Schedule ChangeShedule(int badTime, Schedule schedule)
        {
            int index = GetSecondTopIndex();

            var badReport = _analisedSchedule[badTime][2];
            var goodReport = _analisedSchedule[index][1];

            schedule.Reports.Remove(badReport);
            schedule.Reports.Remove(goodReport);

            // do magic
            var tempBadNumbInSect = badReport.PositionInSection;
            var tempBadSecNumb = badReport.SectionNumber;

            badReport.PositionInSection = goodReport.PositionInSection;
            badReport.SectionNumber = goodReport.SectionNumber;

            goodReport.PositionInSection = tempBadNumbInSect;
            goodReport.SectionNumber = tempBadSecNumb;

            // end do magic

            schedule.Reports.Add(badReport);
            schedule.Reports.Add(goodReport);

            // replace two reports in analised Schedule
            _analisedSchedule[badTime][2] = goodReport;
            _analisedSchedule[index][1] = badReport;

            return schedule;
        }

        /// <summary>
        /// Returns the sum of max values in each row
        /// </summary>
        /// <param name="schedule"></param>
        /// <returns></returns>
        public int GetRating(Schedule schedule)
        {
            AnaliseShedule(schedule);
            var res = 0;
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                res += _reportRatings[_analisedSchedule[i][0].Id];
            }
            return res;
        }

        #region ICommunication members
        public Message GetAnswer()
        {
            return _answer;
        }

        public void ReceiveMessage(Message msg)
        {
            _answer = new Message();
            switch (msg.Type)
            {
                // agent is new. 
                // it exploring the current schedule
                case MessageType.Type.CURRENTSHEDULE:
                    AnaliseShedule(msg.Schedule);
                    if (IsGoodShedule(msg.Schedule))
                    {
                        _answer.Type = MessageType.Type.ACCEPTAGENT;
                    }
                    else
                    {
                        _answer.Schedule = GetMyBestSchedule(msg.Schedule);
                        _answer.Type = MessageType.Type.MODIFIEDSHEDULE;
                    }
                    break;

                // maybe it unnecessary
                case MessageType.Type.REJECTAGENT:
                case MessageType.Type.ACCEPTAGENT:
                    _answer = msg;
                    break;

                default:
                    throw new Exception("Incorrect message for agent");
            }
        }
        #endregion
    }
}
