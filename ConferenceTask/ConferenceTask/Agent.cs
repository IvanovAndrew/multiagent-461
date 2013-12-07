using System.Collections.Generic;
using System.Linq;

namespace ConferenceTask
{
    public class Agent
    {
        public int Id;
        private readonly int[] _ratings;

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

        public Agent(int id, int[] ratings)
        {
            Id = id;
            _ratings = ratings;
            _analisedShedule = new Report [Shedule.ReportsCountInSection][];
            _minBound = CalculateBound();
        }

        /// <summary>
        /// Calculates the lowest threshold
        /// </summary>
        /// <returns></returns>
        private int CalculateBound()
        {
            var temp = _ratings.OrderByDescending(elem => elem).ToList();
            return temp[Shedule.ReportsCountInSection];
        }

        /// <summary>
        /// if agent likes current shedule then returns true
        /// </summary>
        /// <returns></returns>
        public bool IsGoodShedule()
        {
            for (int i = 0; i < Shedule.ReportsCountInSection; i++)
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
            return _ratings[report.Id] >= _minBound;
        }

        public Shedule GetShedule(Shedule shedule)
        {
            var newShedule = new Shedule();
            newShedule = shedule;
            for (int time = 0; time < Shedule.ReportsCountInSection; time++)
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
            for (int i = 0; i < Shedule.ReportsCountInSection; i++)
            {
                var report = _analisedShedule[i][1];
                if (_ratings[report.Id] > max)
                {
                    max = _ratings[report.Id];
                    index = i;
                }
            }
            return index;
        }

        /// <summary>
        /// Changed one report from "bad" time to "good" report 
        /// </summary>
        /// <param name="badTime"></param>
        /// <param name="shedule">planning shedule</param>
        private void ChangeShedule(int badTime, Shedule shedule)
        {
            int index = GetSecondTopIndex();

            var badReport = _analisedShedule[badTime][2];
            var goodReport = _analisedShedule[index][1];

            shedule.Reports.Remove(badReport);
            shedule.Reports.Remove(goodReport);

            // do magic
            var tempBadNumbInSect = badReport.NumberInSection;
            var tempBadSecNumb = badReport.SectionNumber;

            badReport.NumberInSection = goodReport.NumberInSection;
            badReport.SectionNumber = goodReport.SectionNumber;

            goodReport.NumberInSection = tempBadNumbInSect;
            goodReport.SectionNumber = tempBadSecNumb;

            // end do magic

            shedule.Reports.Add(badReport);
            shedule.Reports.Add(goodReport);

            // repalce two reports in analised shedule
            _analisedShedule[badTime][2] = goodReport;
            _analisedShedule[index][1] = badReport;
        }

        /// <summary>
        /// Creates shedule which reports are sorted by descending on each line
        /// </summary>
        /// <param name="shedule">current shedule</param>
        public void AnaliseShedule(Shedule shedule)
        {
            foreach (Report report in shedule.Reports)
            {
                _analisedShedule[report.NumberInSection][report.SectionNumber] = report;
            }

            for (int i = 0; i < Shedule.ReportsCountInSection; i++)
            {
                _analisedShedule[i] = _analisedShedule[i].OrderByDescending(elem => _ratings[elem.Id]).ToArray();
            }
        }
    }
}
