using System;

namespace ConferenceTask
{
    public class Report
    {
        public Report()
        {
            Name = GenerateName();
        }

        //имя доклада. Генирируется Великим Алгоритмом
        public string Name { get; set; }
        //номер секции, в которой находится доклад
        public int SectionNumber { get; set; }
        //порядковый номер доклада в секции
        public int NumberInSection { get; set; }
        //общий порядковый номер доклада
        public int Id { get; set; }

        private const int NameMaxLength = 10;
        private const int NameMinLength = 3;

        /// <summary>
        /// Великий Алгоритм, создающий имя!
        /// </summary>
        /// <returns>
        /// имя объекту! Андрей, если в будущем буду проблемы с выбором имени ребенка, обращайтесь
        /// - OK :)
        /// </returns>
        public string GenerateName()
        {
            var random = new Random();
            var length = random.Next(NameMinLength, NameMaxLength) + 1;
            var name = "";
            for (var letter = 0; letter < length; letter++)
            {
                var ch = (char)('а' + random.Next(32));
                name += ch;
            }
            return name;
        }
    }
}
