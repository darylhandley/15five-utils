package com.sonatype.darylhandley.fifteenfiveutils.commands

interface Command {
    /**
     * Execute the command with the given tokens.
     * @param tokens The tokenized input including the command itself
     */
    fun execute(tokens: List<String>)

    /**
     * Get the usage string for this command.
     * @return A string describing how to use this command
     */
    fun getUsage(): String
}
