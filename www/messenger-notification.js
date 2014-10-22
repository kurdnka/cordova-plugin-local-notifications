var MessengerNotification = function () {
    this._defaults = {
        message:    '',
        title:      '',
        autoCancel: false,
        badge:      0,
        id:         '0',
        json:       ''
    };
};

MessengerNotification.prototype = {
    /**
     * Returns the default settings
     *
     * @return {Object}
     */
    getDefaults: function () {
        return this._defaults;
    },

    /**
     * Overwrite default settings
     *
     * @param {Object} defaults
     */
    setDefaults: function (newDefaults) {
        var defaults = this.getDefaults();

        for (var key in defaults) {
            if (newDefaults[key] !== undefined) {
                defaults[key] = newDefaults[key];
            }
        }
    },

    /**
     * @private
     *
     * Merges custom properties with the default values.
     *
     * @param {Object} options
     *      Set of custom values
     *
     * @retrun {Object}
     *      The merged property list
     */
    mergeWithDefaults: function (options) {
        var defaults = this.getDefaults();

        for (var key in defaults) {
            if (options[key] === undefined) {
                options[key] = defaults[key];
            }
        }

        return options;
    },

    /**
     * @private
     *
     * Merges the platform specific properties into the default properties.
     *
     * @return {Object}
     *      The default properties for the platform
     */
    applyPlatformSpecificOptions: function () {
        var defaults = this._defaults;

        switch (device.platform) {
        case 'Android':
            defaults.icon       = 'icon';
            defaults.smallIcon  = null;
        }

        return defaults;
    },

    /**
     * @private
     *
     * Creates a callback, which will be executed within a specific scope.
     *
     * @param {Function} callbackFn
     *      The callback function
     * @param {Object} scope
     *      The scope for the function
     *
     * @return {Function}
     *      The new callback function
     */
    createCallbackFn: function (callbackFn, scope) {
        if (typeof callbackFn != 'function')
            return;

        return function () {
            callbackFn.apply(scope || this, arguments);
        };
    },

    /**
     * Add a new entry to the registry
     *
     * @param {Object} options
     *      The notification properties
     * @param {Function} callback
     *      A function to be called after the notification has been canceled
     * @param {Object} scope
     *      The scope for the callback function
     *
     * @return {Number}
     *      The notification's ID
     */
    add: function (options, callback, scope) {
        var options    = this.mergeWithDefaults(options),
            callbackFn = this.createCallbackFn(callback, scope);

        cordova.exec(callbackFn, null, 'MessengerNotification', 'add', [options]);
    },

    /**
     * Cancels the specified notification.
     *
     * @param {String} id
     *      The ID of the notification
     * @param {Function} callback
     *      A function to be called after the notification has been canceled
     * @param {Object} scope
     *      The scope for the callback function
     */
    cancel: function (tag, callback, scope) {
        var callbackFn = this.createCallbackFn(callback, scope);

        cordova.exec(callbackFn, null, 'MessengerNotification', 'cancel', [tag]);
    },

    /**
     * Removes all previously registered notifications.
     *
     * @param {Function} callback
     *      A function to be called after all notifications have been canceled
     * @param {Object} scope
     *      The scope for the callback function
     */
    cancelAll: function (callback, scope) {
        var callbackFn = this.createCallbackFn(callback, scope);

        cordova.exec(callbackFn, null, 'MessengerNotification', 'cancelAll', []);
    },

    /**
     * Occurs when a notification was added.
     *
     * @param {String} id
     *      The ID of the notification
     * @param {String} state
     *      Either "foreground" or "background"
     * @param {String} json
     *      A custom (JSON) string
     */
    onadd: function (id, state, json) {},

    /**
     * Occurs when the notification is triggered.
     *
     * @param {String} id
     *      The ID of the notification
     * @param {String} state
     *      Either "foreground" or "background"
     * @param {String} json
     *      A custom (JSON) string
     */
    ontrigger: function (id, state, json) {},

    /**
     * Fires after the notification was clicked.
     *
     * @param {String} id
     *      The ID of the notification
     * @param {String} state
     *      Either "foreground" or "background"
     * @param {String} json
     *      A custom (JSON) string
     */
    onclick: function (id, state, json) {},

    /**
     * Fires if the notification was canceled.
     *
     * @param {String} id
     *      The ID of the notification
     * @param {String} state
     *      Either "foreground" or "background"
     * @param {String} json
     *      A custom (JSON) string
     */
    oncancel: function (id, state, json) {}
};

var plugin  = new MessengerNotification();

module.exports = plugin;
