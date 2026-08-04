package com.tm.tsm_atelier.domain.user.service;

import com.tm.tsm_atelier.common.exception.custom.AddressLimitExceededException;
import com.tm.tsm_atelier.common.exception.custom.AddressNotFoundException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.user.dto.AddressRequestDTO;
import com.tm.tsm_atelier.domain.user.dto.AddressResponseDTO;
import com.tm.tsm_atelier.domain.user.entity.Address;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.AddressRepository;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import com.tm.tsm_atelier.domain.user.utils.PostalCodeUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

	private final AddressRepository addressRepository;
	private final UserRepository userRepository;

	public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
		this.addressRepository = addressRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<AddressResponseDTO> findAllByUser(User user) {
		return addressRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream().map(this::toResponseDTO)
				.toList();
	}

	@Transactional
	public AddressResponseDTO create(User user, AddressRequestDTO request) {
		userRepository.findByIdWithPessimisticLock(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("User", user.getId()));

		long addressCount = addressRepository.countByUserId(user.getId());

		if (addressCount >= 5) {
			throw new AddressLimitExceededException("You can only have up to 5 addresses.");
		}

		boolean isDefault = request.isDefault();
		if (addressCount == 0) {
			isDefault = true; // First address is always default
		} else if (isDefault) {
			clearCurrentDefault(user.getId());
		}

		Address address = Address.builder().user(user).street(request.street()).number(request.number())
				.complement(request.complement()).neighborhood(request.neighborhood()).city(request.city())
				.state(request.state()).postalCode(PostalCodeUtils.formatPostalCode(request.postalCode()))
				.isDefault(isDefault).build();

		Address savedAddress = addressRepository.save(address);
		return toResponseDTO(savedAddress);
	}

	@Transactional
	public AddressResponseDTO update(User user, Long addressId, AddressRequestDTO request) {
		Address address = getAddressBelongingToUser(user.getId(), addressId);

		if (request.isDefault() && !address.isDefault()) {
			clearCurrentDefault(user.getId());
			address.setDefault(true);
		} else if (!request.isDefault() && address.isDefault()) {
			address.setDefault(false);
		}

		address.setStreet(request.street());
		address.setNumber(request.number());
		address.setComplement(request.complement());
		address.setNeighborhood(request.neighborhood());
		address.setCity(request.city());
		address.setState(request.state());
		address.setPostalCode(PostalCodeUtils.formatPostalCode(request.postalCode()));

		return toResponseDTO(addressRepository.save(address));
	}

	@Transactional
	public AddressResponseDTO setDefault(User user, Long addressId) {
		Address address = getAddressBelongingToUser(user.getId(), addressId);

		if (!address.isDefault()) {
			clearCurrentDefault(user.getId());
			address.setDefault(true);
			addressRepository.save(address);
		}

		return toResponseDTO(address);
	}

	@Transactional
	public void delete(User user, Long addressId) {
		Address address = getAddressBelongingToUser(user.getId(), addressId);
		boolean wasDefault = address.isDefault();

		addressRepository.delete(address);

		// O índice uk_addresses_user_default só permite uma linha com is_default =
		// true por usuário. Como o Hibernate ordena UPDATEs antes de DELETEs dentro
		// de um mesmo flush, promover o próximo endereço sem forçar o DELETE antes
		// deixaria duas linhas default ao mesmo tempo e violaria o índice.
		addressRepository.flush();

		if (wasDefault) {
			List<Address> remainingAddresses = addressRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
			if (!remainingAddresses.isEmpty()) {
				Address newDefault = remainingAddresses.get(0);
				newDefault.setDefault(true);
				addressRepository.save(newDefault);
			}
		}
	}

	private Address getAddressBelongingToUser(UUID userId, Long addressId) {
		Address address = addressRepository.findById(addressId)
				.orElseThrow(() -> new AddressNotFoundException("Address not found."));

		if (!address.getUser().getId().equals(userId)) {
			throw new AddressNotFoundException("Address not found.");
		}

		return address;
	}

	private void clearCurrentDefault(UUID userId) {
		Optional<Address> currentDefault = addressRepository.findByUserIdAndIsDefaultTrue(userId);
		currentDefault.ifPresent(addr -> {
			addr.setDefault(false);
			addressRepository.saveAndFlush(addr);
		});
	}

	private AddressResponseDTO toResponseDTO(Address address) {
		return new AddressResponseDTO(address.getId(), address.getStreet(), address.getNumber(),
				address.getComplement(), address.getNeighborhood(), address.getCity(), address.getState(),
				address.getPostalCode(), address.isDefault());
	}
}
