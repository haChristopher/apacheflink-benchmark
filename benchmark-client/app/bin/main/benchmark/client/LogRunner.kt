class LogRunner: Runnable {
    public override fun run() {
        println("${Thread.currentThread()} has run.")
    }
}