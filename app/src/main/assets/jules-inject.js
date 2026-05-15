(function() {
    // Note: CSS content will be injected separately from Kotlin for better control
    
    window.syncTheme = function(isDark) {
        const btn = document.querySelector('.ui-color-mode');
        if (!btn) {
            setTimeout(() => window.syncTheme(isDark), 500);
            return;
        }
        const icon = btn.querySelector('mat-icon');
        if (!icon) return;
        const iconText = icon.textContent.trim();
        const isSiteDark = (iconText === 'light_mode'); 
        const isDarkByClass = document.body.classList.contains('dark-theme') || 
                             document.documentElement.classList.contains('dark');
        const actuallyDark = isSiteDark || isDarkByClass;
        
        if (isDark !== actuallyDark) {
            btn.click();
        }
    };

    if (window.JulesSwipeInitialized) return;
    window.JulesSwipeInitialized = true;
    
    let startX = 0;
    let startY = 0;
    let lastActionTime = 0;
    
    window.addEventListener('touchstart', function(e) {
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
    }, {passive: true, capture: true});
    
    window.addEventListener('touchend', function(e) {
        let now = Date.now();
        if (now - lastActionTime < 500) return;
        let endX = e.changedTouches[0].clientX;
        let endY = e.changedTouches[0].clientY;
        let dx = endX - startX;
        let dy = Math.abs(endY - startY);
        
        if (Math.abs(dx) > 80 && dy < 100) {
            let panel = document.getElementById('start-panel');
            let btn = document.querySelector('button.start-panel-button.is-left');
            if (!btn) return;
            let isClosed = !panel || panel.classList.contains('closed');
            if (dx > 0 && isClosed) {
                btn.click();
                lastActionTime = now;
            } else if (dx < 0 && !isClosed) {
                btn.click();
                lastActionTime = now;
            }
        }
    }, {passive: true, capture: true});
})();