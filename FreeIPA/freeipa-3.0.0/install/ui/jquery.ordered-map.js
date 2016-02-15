/*  Authors:
 *    Endi Dewata <edewata@redhat.com>
 *
 * Copyright (C) 2010 Red Hat
 * see file 'COPYING' for use and warranty information
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

jQuery.ordered_map = jQuery.fn.ordered_map = function(map) {

    var that = {};

    /**
     * These variables can be read directly but should not be
     * modified directly. Use the provided methods instead.
     */
    that.keys = [];
    that.values = [];
    that.map = {};
    that.length = 0;

    that.get = function(key) {
        return that.map[key];
    };

    that.put = function(key, value) {

        var i = that.get_key_index(key);
        if (i >= 0) {
            that.values[i] = value;
        } else {
            that.keys.push(key);
            that.values.push(value);
            that.length = that.keys.length;
        }

        that.map[key] = value;

        return that;
    };

    that.put_map = function(map) {

        if (typeof map !== 'object') return that;

        for (name in map) {

            if (map.hasOwnProperty(name)) {
                that.put(name, map[name]);
            }
        }

        return that;
    };

    that.put_array = function(array, key_name, operation) {

        var i, item, type, key;

        array = array || [];

        for (i=0; i<array.length; i++) {
            item = array[i];
            type = typeof item;
            if (type === 'string') {
                key = item;
            } if (type === 'object') {
                key = item[key_name];
            }

            if (operation) {
                item = operation(item);
            }

            if (key) {
                that.put(key, item);
            }
        }

        return that;
    };

    that.remove = function(key) {

        var i = that.get_key_index(key);
        if (i<0) return null;

        that.keys.splice(i, 1);
        that.values.splice(i, 1);

        var value = that.map[key];
        delete that.map[key];
        that.length = that.keys.length;
        return value;
    };

    that.empty = function() {
        that.keys = [];
        that.values = [];
        that.map = {};
        that.length = that.keys.length;
        return that;
    };

    that.get_key_index = function(key) {
        return that.keys.indexOf(key);
    };

    that.get_key_by_index = function(index) {
        return that.keys[index];
    };

    that.get_value_by_index = function(index) {
        return that.values[index];
    };

    that.sort = function() {
        var keys = that.keys.slice(0);
        keys.sort();
        return that.trim(keys);
    };

    that.slice = function(start, end) {
        var keys = that.keys.slice(start, end);
        return that.trim(keys);
    };

    that.trim = function(keys) {
        var new_map = $.ordered_map();

        for (var i=0; i<keys.length; i++) {
            var key = keys[i];
            var value = that.get(key);
            new_map.put(key, value);
        }

        return new_map;
    };

    that.put_map(map);

    return that;
};
