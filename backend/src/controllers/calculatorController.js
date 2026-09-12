import db from '../config/db.js';

/**
 * 1. Leave-Day Calculator
 * Calculates how many upcoming classes a student can miss without dropping below target attendance %.
 */
export function calculateLeaveDays(req, res) {
  try {
    let { presentClasses, totalClasses, desiredPercentage = 75 } = req.body;

    presentClasses = Number(presentClasses);
    totalClasses = Number(totalClasses);
    desiredPercentage = Number(desiredPercentage);

    if (isNaN(presentClasses) || isNaN(totalClasses) || isNaN(desiredPercentage) || totalClasses <= 0) {
      return res.status(400).json({
        success: false,
        error: 'Valid numbers for presentClasses, totalClasses, and desiredPercentage are required.'
      });
    }

    const currentPercentage = Math.round((presentClasses / totalClasses) * 1000) / 10;
    const k = desiredPercentage / 100;

    // Formula: L <= (P - k*T) / k
    const maxMissable = Math.floor((presentClasses - k * totalClasses) / k);

    if (maxMissable > 0) {
      const finalTotal = totalClasses + maxMissable;
      const finalPercentage = Math.round((presentClasses / finalTotal) * 1000) / 10;

      return res.status(200).json({
        success: true,
        currentPercentage,
        desiredPercentage,
        canTakeLeave: true,
        maxMissableClasses: maxMissable,
        projectedTotalClasses: finalTotal,
        projectedAttendancePercentage: finalPercentage,
        message: `You can miss approximately ${maxMissable} class${maxMissable > 1 ? 'es' : ''} while maintaining ${finalPercentage}% (>= ${desiredPercentage}%) attendance.`
      });
    } else {
      return res.status(200).json({
        success: true,
        currentPercentage,
        desiredPercentage,
        canTakeLeave: false,
        maxMissableClasses: 0,
        message: `⚠️ No additional leave is available. Current attendance is ${currentPercentage}%. Missing any classes will cause attendance to fall below ${desiredPercentage}%.`
      });
    }
  } catch (error) {
    console.error('Leave calculator error:', error);
    return res.status(500).json({ success: false, error: 'Failed to compute leave calculator.' });
  }
}

/**
 * 2. Skip-Class Calculator
 * Simulates skipping N classes and calculates the exact resulting percentage.
 */
export function calculateSkipClasses(req, res) {
  try {
    let { presentClasses, totalClasses, targetPercentage = 75, skipCount = 1 } = req.body;

    presentClasses = Number(presentClasses);
    totalClasses = Number(totalClasses);
    targetPercentage = Number(targetPercentage);
    skipCount = Number(skipCount);

    if (isNaN(presentClasses) || isNaN(totalClasses) || totalClasses <= 0) {
      return res.status(400).json({ success: false, error: 'Invalid input numbers.' });
    }

    const currentPercentage = Math.round((presentClasses / totalClasses) * 1000) / 10;
    const k = targetPercentage / 100;

    // Max classes to skip before going below target
    const maxSkippable = Math.max(0, Math.floor((presentClasses - k * totalClasses) / k));

    // Impact of user's chosen skipCount
    const newTotal = totalClasses + skipCount;
    const newPercentage = Math.round((presentClasses / newTotal) * 1000) / 10;
    const willFallBelowTarget = newPercentage < targetPercentage;

    // Scenario table for skipping 1 to 5 classes
    const scenarios = [1, 2, 3, 4, 5].map(cnt => {
      const tot = totalClasses + cnt;
      const pct = Math.round((presentClasses / tot) * 1000) / 10;
      return {
        classesSkipped: cnt,
        resultingPercentage: pct,
        isSafe: pct >= targetPercentage
      };
    });

    return res.status(200).json({
      success: true,
      currentPercentage,
      targetPercentage,
      maxClassesYouCanSkip: maxSkippable,
      simulatedSkipCount: skipCount,
      expectedAttendanceAfterSkip: newPercentage,
      willFallBelowTarget,
      scenarios,
      message: willFallBelowTarget
        ? `⚠️ Skipping ${skipCount} classes will reduce your attendance to ${newPercentage}%, below your ${targetPercentage}% target.`
        : `✅ Skipping ${skipCount} classes will keep your attendance at ${newPercentage}%, safely above your ${targetPercentage}% target.`
    });
  } catch (error) {
    console.error('Skip calculator error:', error);
    return res.status(500).json({ success: false, error: 'Failed to compute skip calculator.' });
  }
}

/**
 * 3. Target Attendance Calculator
 * Calculates minimum consecutive classes student must attend to reach target %.
 */
export function calculateTargetAttendance(req, res) {
  try {
    let { presentClasses, totalClasses, targetPercentage = 75 } = req.body;

    presentClasses = Number(presentClasses);
    totalClasses = Number(totalClasses);
    targetPercentage = Number(targetPercentage);

    if (isNaN(presentClasses) || isNaN(totalClasses) || totalClasses <= 0 || targetPercentage <= 0 || targetPercentage >= 100) {
      return res.status(400).json({ success: false, error: 'Target percentage must be between 1 and 99.' });
    }

    const currentPercentage = Math.round((presentClasses / totalClasses) * 1000) / 10;

    if (currentPercentage >= targetPercentage) {
      return res.status(200).json({
        success: true,
        currentPercentage,
        targetPercentage,
        targetAlreadyAchieved: true,
        requiredConsecutiveClasses: 0,
        message: `🎉 Great job! Your current attendance (${currentPercentage}%) already exceeds your target of ${targetPercentage}%.`
      });
    }

    const k = targetPercentage / 100;
    // Formula: C >= (k*T - P) / (1 - k)
    const requiredClasses = Math.ceil((k * totalClasses - presentClasses) / (1 - k));

    const finalPresent = presentClasses + requiredClasses;
    const finalTotal = totalClasses + requiredClasses;
    const achievedPercentage = Math.round((finalPresent / finalTotal) * 1000) / 10;

    return res.status(200).json({
      success: true,
      currentPercentage,
      targetPercentage,
      targetAlreadyAchieved: false,
      requiredConsecutiveClasses: requiredClasses,
      projectedPresent: finalPresent,
      projectedTotal: finalTotal,
      achievedPercentage,
      message: `You must attend ${requiredClasses} consecutive class${requiredClasses > 1 ? 'es' : ''} to reach ${achievedPercentage}% attendance.`
    });
  } catch (error) {
    console.error('Target calculator error:', error);
    return res.status(500).json({ success: false, error: 'Failed to compute target attendance.' });
  }
}

/**
 * 4. Exact Day Planner
 * Estimates academic days and calendar timeline to reach target attendance.
 */
export function calculateAttendancePlanner(req, res) {
  try {
    let { presentClasses, totalClasses, targetPercentage = 75, avgClassesPerDay = 5.0 } = req.body;

    presentClasses = Number(presentClasses);
    totalClasses = Number(totalClasses);
    targetPercentage = Number(targetPercentage);
    avgClassesPerDay = Number(avgClassesPerDay) || 5.0;

    const currentPercentage = Math.round((presentClasses / totalClasses) * 1000) / 10;
    const k = targetPercentage / 100;
    const requiredClasses = currentPercentage >= targetPercentage 
      ? 0 
      : Math.ceil((k * totalClasses - presentClasses) / (1 - k));

    const academicDaysRequired = Math.ceil(requiredClasses / avgClassesPerDay);

    // Fetch upcoming holidays from database
    const upcomingHolidays = db.prepare(`
      SELECT date, holiday_name, day_of_week 
      FROM academic_calendar 
      WHERE is_holiday = 1 AND date >= CURRENT_DATE 
      ORDER BY date ASC 
      LIMIT 10
    `).all();

    // Generate mini roadmap visualization items
    const roadmap = [];
    let accumulatedClasses = 0;
    for (let day = 1; day <= Math.max(academicDaysRequired, 1); day++) {
      accumulatedClasses = Math.min(requiredClasses, accumulatedClasses + Math.round(avgClassesPerDay));
      const simulatedPresent = presentClasses + accumulatedClasses;
      const simulatedTotal = totalClasses + accumulatedClasses;
      const simulatedPct = Math.round((simulatedPresent / simulatedTotal) * 1000) / 10;

      roadmap.push({
        dayNumber: day,
        classesAttendedOnDay: Math.min(Math.round(avgClassesPerDay), requiredClasses - (accumulatedClasses - Math.round(avgClassesPerDay))),
        totalClassesAttendedSoFar: accumulatedClasses,
        projectedPercentage: simulatedPct,
        status: simulatedPct >= targetPercentage ? 'TARGET_REACHED' : 'IN_PROGRESS'
      });
      if (simulatedPct >= targetPercentage && day >= academicDaysRequired) break;
    }

    return res.status(200).json({
      success: true,
      currentPercentage,
      targetPercentage,
      requiredConsecutiveClasses: requiredClasses,
      avgClassesPerDay,
      estimatedAcademicDaysRequired: academicDaysRequired,
      estimatedCalendarDays: Math.ceil(academicDaysRequired * 1.4), // Accounting for weekends
      upcomingHolidays,
      roadmap,
      summary: {
        classesRequired: requiredClasses,
        daysRequired: academicDaysRequired,
        formattedMessage: requiredClasses === 0 
          ? 'Target already reached.'
          : `📅 Estimated Days: ${academicDaysRequired} day${academicDaysRequired > 1 ? 's' : ''} | 📚 Classes Required: ${requiredClasses}`
      }
    });
  } catch (error) {
    console.error('Planner error:', error);
    return res.status(500).json({ success: false, error: 'Failed to compute attendance planner.' });
  }
}

/**
 * 5. Holiday Impact Preview
 * Analyzes the impact of holidays on attendance projection.
 */
export function calculateHolidayImpact(req, res) {
  try {
    let { presentClasses, totalClasses, targetPercentage = 75, holidaysCount = 3, avgClassesPerDay = 5.0 } = req.body;

    presentClasses = Number(presentClasses);
    totalClasses = Number(totalClasses);
    targetPercentage = Number(targetPercentage);
    holidaysCount = Number(holidaysCount);
    avgClassesPerDay = Number(avgClassesPerDay) || 5.0;

    const currentPercentage = Math.round((presentClasses / totalClasses) * 1000) / 10;
    const classesLostDueToHoliday = Math.round(holidaysCount * avgClassesPerDay);

    // Baseline required without holidays
    const k = targetPercentage / 100;
    const baseRequiredClasses = currentPercentage >= targetPercentage 
      ? 0 
      : Math.ceil((k * totalClasses - presentClasses) / (1 - k));
    const baseDays = Math.ceil(baseRequiredClasses / avgClassesPerDay);

    // With holidays postponing academic classes:
    const adjustedCalendarDays = baseDays + holidaysCount + Math.ceil((baseDays + holidaysCount) / 5) * 2;

    return res.status(200).json({
      success: true,
      currentAttendance: currentPercentage,
      targetAttendance: targetPercentage,
      holidaysCount,
      classesSuspended: classesLostDueToHoliday,
      academicDaysBeforeTarget: baseDays,
      calendarDaysWithHolidays: adjustedCalendarDays,
      holidayImpactSummary: `Adding ${holidaysCount} holidays extends the recovery timeline by ${holidaysCount} academic day${holidaysCount > 1 ? 's' : ''} (${classesLostDueToHoliday} potential classes postponed).`
    });
  } catch (error) {
    console.error('Holiday impact error:', error);
    return res.status(500).json({ success: false, error: 'Failed to compute holiday impact.' });
  }
}
