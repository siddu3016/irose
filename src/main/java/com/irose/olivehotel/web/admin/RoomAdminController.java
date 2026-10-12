package com.irose.olivehotel.web.admin;

import com.irose.olivehotel.domain.Room;
import com.irose.olivehotel.domain.RoomStatus;
import com.irose.olivehotel.domain.RoomType;
import com.irose.olivehotel.repository.RoomRepository;
import com.irose.olivehotel.repository.RoomTypeRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class RoomAdminController {

    private final RoomRepository rooms;
    private final RoomTypeRepository roomTypes;

    public RoomAdminController(RoomRepository rooms, RoomTypeRepository roomTypes) {
        this.rooms = rooms;
        this.roomTypes = roomTypes;
    }

    // ---------- Rooms ----------

    @GetMapping("/rooms")
    public String rooms(Model model) {
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumberAsc());
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByPricePerNightAsc());
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("active", "rooms");
        return "admin/rooms";
    }

    @PostMapping("/rooms")
    public String addRoom(@RequestParam String roomNumber, @RequestParam(required = false) Integer floor,
                          @RequestParam Long roomTypeId, RedirectAttributes flash) {
        String number = roomNumber.trim();
        if (number.isEmpty() || rooms.existsByRoomNumber(number)) {
            flash.addFlashAttribute("error", "Room number is empty or already exists.");
            return "redirect:/admin/rooms";
        }
        Room r = new Room();
        r.setRoomNumber(number);
        r.setFloor(floor);
        r.setRoomType(roomTypes.findById(roomTypeId).orElseThrow());
        rooms.save(r);
        flash.addFlashAttribute("success", "Room " + number + " added.");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/rooms/{id}/status")
    public String setStatus(@PathVariable Long id, @RequestParam RoomStatus status,
                            @RequestParam(defaultValue = "/admin/rooms") String back, RedirectAttributes flash) {
        Room r = rooms.findById(id).orElseThrow();
        r.setStatus(status);
        rooms.save(r);
        flash.addFlashAttribute("success", "Room " + r.getRoomNumber() + " marked " + status.name().toLowerCase() + ".");
        return "redirect:" + (back.startsWith("/admin") ? back : "/admin/rooms");
    }

    @PostMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes flash) {
        try {
            rooms.deleteById(id);
            rooms.flush();
            flash.addFlashAttribute("success", "Room deleted.");
        } catch (DataIntegrityViolationException e) {
            flash.addFlashAttribute("error", "This room has bookings and cannot be deleted. Mark it as maintenance instead.");
        }
        return "redirect:/admin/rooms";
    }

    // ---------- Room types ----------

    @GetMapping("/room-types")
    public String roomTypes(Model model) {
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByPricePerNightAsc());
        model.addAttribute("active", "roomTypes");
        return "admin/room-types";
    }

    @GetMapping("/room-types/new")
    public String newType(Model model) {
        model.addAttribute("roomType", new RoomType());
        model.addAttribute("active", "roomTypes");
        return "admin/room-type-form";
    }

    @GetMapping("/room-types/{id}")
    public String editType(@PathVariable Long id, Model model) {
        model.addAttribute("roomType", roomTypes.findById(id).orElseThrow());
        model.addAttribute("active", "roomTypes");
        return "admin/room-type-form";
    }

    @PostMapping("/room-types")
    public String saveType(@Valid @ModelAttribute("roomType") RoomType roomType, BindingResult result,
                           Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("active", "roomTypes");
            return "admin/room-type-form";
        }
        try {
            roomTypes.saveAndFlush(roomType);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("active", "roomTypes");
            model.addAttribute("error", "A room type with that name already exists.");
            return "admin/room-type-form";
        }
        flash.addFlashAttribute("success", "Room type saved. New prices apply to new bookings.");
        return "redirect:/admin/room-types";
    }
}
