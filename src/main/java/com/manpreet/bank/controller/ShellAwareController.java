package com.manpreet.bank.controller;

/**
 * Controllers hosted inside the authenticated shell can request a parent refresh.
 */
public interface ShellAwareController {

    void setShellController(MainShellController shellController);
}
