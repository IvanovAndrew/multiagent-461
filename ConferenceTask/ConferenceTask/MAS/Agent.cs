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
        private readonly Report[][] _analisedShedule;

        private Message answer;

        public Agent(int id, int[] reportRatings)
        {
            Id = id;
            _reportRatings = reportRatings;
            _analisedShedule = new Report[Schedule.ReportsCountInSection][];
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                _analisedShedule[i] = new Report[Schedule.Sections];
            }
            
            _minBound = CalculateBound();
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

        /// <summary>
        /// if agent likes current Schedule then returns true
        /// </summary>
        /// <returns></returns>
        public bool IsGoodShedule()
        {
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                if (IsGoodTime(i)) continue;
                    return false;
            }
            return true;
        }

        /// <summary>
        /// Checks if maximal rating in the time is greather than minimal bound
        /// </summary>
        /// <param name="time"></param>
        /// <returns></returns>
        private bool IsGoodTime(int time)
        {
            var report = _analisedShedule[time][0];
            return _reportRatings[report.Id] >= _minBound;
        }

        public Schedule GetMyBestShedule(Schedule schedule)
        {
            var newShedule = new Schedule();
            newShedule = schedule;
            for (int time = 0; time < Schedule.ReportsCountInSection; time++)
            {
                if (IsGoodTime(time)) continue;

                ChangeShedule(time, newShedule);
            }
            return newShedule;
        }

        private int GetSecondTopIndex()
        {
            var max = -1;
            var index = -1;
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                var report = _analisedShedule[i][1];
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
        private void ChangeShedule(int badTime, Schedule schedule)
        {
            int index = GetSecondTopIndex();

            var badReport = _analisedShedule[badTime][2];
            var goodReport = _analisedShedule[index][1];

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

            // repalce two reports in analised Schedule
            _analisedShedule[badTime][2] = goodReport;
            _analisedShedule[index][1] = badReport;
        }

        /// <summary>
        /// Creates Schedule which reports are sorted by descending on each line
        /// </summary>
        /// <param name="schedule">current Schedule</param>
        public void AnaliseShedule(Schedule schedule)
        {
            foreach (Report report in schedule.Reports)
            {
                _analisedShedule[report.PositionInSection][report.SectionNumber] = report;
            }

            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                _analisedShedule[i] = _analisedShedule[i].OrderByDescending(elem => _reportRatings[elem.Id]).ToArray();
            }
        }

        public int GetRating(Schedule schedule)
        {
            AnaliseShedule(schedule);
            int res = 0;
            for (int i = 0; i < Schedule.ReportsCountInSection; i++)
            {
                res += _reportRatings[_analisedShedule[i][0].Id];
            }
            return res;
        }

        #region ICommunication members
        public Message GetAnswer()
        {
            return answer;
        }

        public void ReceiveMessage(Message msg)
        {
            answer = new Message();
            switch (msg.Type)
            {
                // agent is new. 
                // it exploring the current schedule
                case MessageType.Type.CURRENTSHEDULE:
                    AnaliseShedule(msg.Schedule);
                    if (IsGoodShedule())
                    {
                        answer.Type = MessageType.Type.ACCEPTAGENT;
                    }
                    else
                    {           
                        answer.Type = MessageType.Type.MODIFIEDSHEDULE;
                        answer.Schedule = GetMyBestShedule(msg.Schedule);
                    }
                    break;

                // maybe it unnecessary
                case MessageType.Type.REJECTAGENT:
                case MessageType.Type.ACCEPTAGENT:
                    answer = msg;
                    break;

                default:
                    throw new Exception("Incorrect message for agent");
                    break;
            }


        }
        #endregion
    }
}
