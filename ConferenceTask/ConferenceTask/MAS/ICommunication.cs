namespace ConferenceTask.MAS
{
    interface ICommunication
    {
        Message GetAnswer();

        void ReceiveMessage(Message msg);
    }
}