using System.Collections.Generic;

namespace ConferenceTask
{
    public class Shedule
    {
        //количество секций
        public const int Sections = 3;
        //количество докладов в одной секции
        public const int ReportsCountInSection = 10;
        //общее количество докладов
        public const int ReportsCount = Sections*ReportsCountInSection;

        //список докладов
        public List<Report> Reports { get; set; }

        public Shedule()
        {
            Reports = new List<Report>();
        }
    }
}
