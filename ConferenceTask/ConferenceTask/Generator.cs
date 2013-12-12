using System;
using System.IO;

namespace ConferenceTask
{
    class Generator
    {
        /// <summary>
        /// количество слушателей
        /// </summary>
        public const int Listeners = 1050;
        /// <summary>
        /// количество потоков
        /// </summary>
        public const int Sections = 3;
        /// <summary>
        /// количество докладов
        /// </summary>
        public const int Reports = 30;
        /// <summary>
        /// путь к файлу с входной матрицей
        /// </summary>
        public const string FilePath = "input.txt";

        private static readonly int[,] Matrix = new int[Reports,Listeners];

        public static readonly string[] Separator = new[]{" "};

        /// <summary>
        /// генерирует и записывает матрицу в файл
        /// </summary>
        public static void GenerateMatrix()
        {
            var random = new Random();
            using (var writer = new StreamWriter(FilePath))
            {
                for (int topic = 0; topic < Reports; topic++)
                {
                    for (int listener = 0; listener < Listeners; listener++)
                    {
                        writer.Write("{0}", random.Next(10) + Separator[0]);
                    }
                    writer.WriteLine();
                }
            }
        }

        /// <summary>
        /// считывает матрицу из файла
        /// </summary>
        /// <returns>
        /// входная матрица из файла
        /// </returns>
        public static int[,] ReadMatrixFromFile()
        {
            var matrix = new int[Reports, Listeners];
            using (var reader = new StreamReader(FilePath))
            {
                for  (int topic = 0; topic < Reports; topic++) 
                {
                    string line;
                    if ((line = reader.ReadLine()) == null) throw new Exception("Incorrect file!");
                    var inputArray = line.Split(Separator, StringSplitOptions.RemoveEmptyEntries);
                    if(inputArray.Length != Listeners) throw  new Exception("Incorrect file!");
                    for (int listener = 0; listener < Listeners; listener++)
                    {
                        int res;
                        int.TryParse(inputArray[listener], out res);
                        matrix[topic, listener] = res;
                    }
                }
            }
            return matrix;
        }

        public static string GenerateName(Random random)
        {
            var length = random.Next(3, 8);
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
