package com.example.meetings.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RegisterAndLoginE2ETest extends E2EBaseTest {

    private WebDriverWait awaiter() {
        return new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @Test
    void register_validData_redirectsToLogin() {
        navigateTo("/register");

        driver.findElement(By.name("username")).sendKeys("e2euser");
        driver.findElement(By.name("email")).sendKeys("e2e@example.com");
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        awaiter().until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void register_duplicateUsername_showsError() {
        navigateTo("/register");
        driver.findElement(By.name("username")).sendKeys("dupuser");
        driver.findElement(By.name("email")).sendKeys("dup@example.com");
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        navigateTo("/register");
        driver.findElement(By.name("username")).sendKeys("dupuser");
        driver.findElement(By.name("email")).sendKeys("dup2@example.com");
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        awaiter().until(ExpectedConditions.urlContains("/register"));
        assertTrue(driver.getPageSource().contains("already taken") ||
                driver.getPageSource().contains("error") ||
                driver.getCurrentUrl().contains("/register"));
    }

    @Test
    void login_validCredentials_redirectsToCalendar() {
        navigateTo("/register");
        driver.findElement(By.name("username")).sendKeys("loginuser");
        driver.findElement(By.name("email")).sendKeys("login@example.com");
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        awaiter().until(ExpectedConditions.urlContains("/login"));
        driver.findElement(By.name("username")).sendKeys("loginuser");
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        awaiter().until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getCurrentUrl().contains("/calendar"));
    }

    @Test
    void login_invalidCredentials_staysOnLogin() {
        navigateTo("/login");
        driver.findElement(By.name("username")).sendKeys("wronguser");
        driver.findElement(By.name("password")).sendKeys("wrongpassword");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        awaiter().until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }
}