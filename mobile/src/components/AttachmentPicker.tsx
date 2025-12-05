import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Modal,
  ActivityIndicator,
  Image,
  Alert,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import { Ionicons } from '@expo/vector-icons';
import { colors, spacing, borderRadius, fontSize } from '../constants/theme';
import { api } from '../services/api';
import type { Attachment } from '../types';

interface AttachmentPickerProps {
  visible: boolean;
  onClose: () => void;
  conversationId: string;
  onAttachmentReady: (attachment: Attachment, localUri: string) => void;
}

type AttachmentOption = 'camera' | 'gallery' | 'document';

export function AttachmentPicker({ 
  visible, 
  onClose, 
  conversationId,
  onAttachmentReady 
}: AttachmentPickerProps) {
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const handleOption = async (option: AttachmentOption) => {
    try {
      let result: ImagePicker.ImagePickerResult | DocumentPicker.DocumentPickerResult | null = null;

      if (option === 'camera') {
        const permission = await ImagePicker.requestCameraPermissionsAsync();
        if (!permission.granted) {
          Alert.alert('Permission needed', 'Please grant camera access to take photos');
          return;
        }
        result = await ImagePicker.launchCameraAsync({
          mediaTypes: ImagePicker.MediaTypeOptions.All,
          quality: 0.8,
          allowsEditing: true,
        });
      } else if (option === 'gallery') {
        const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
        if (!permission.granted) {
          Alert.alert('Permission needed', 'Please grant gallery access to select photos');
          return;
        }
        result = await ImagePicker.launchImageLibraryAsync({
          mediaTypes: ImagePicker.MediaTypeOptions.All,
          quality: 0.8,
          allowsEditing: true,
        });
      } else if (option === 'document') {
        result = await DocumentPicker.getDocumentAsync({
          type: [
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'text/plain',
          ],
          copyToCacheDirectory: true,
        });
      }

      if (!result) return;

      // Handle ImagePicker result
      if ('canceled' in result) {
        if (result.canceled) return;
        
        const asset = result.assets[0];
        await uploadFile({
          uri: asset.uri,
          name: asset.fileName || `attachment_${Date.now()}.${asset.uri.split('.').pop()}`,
          type: asset.mimeType || 'image/jpeg',
          size: asset.fileSize || 0,
        });
      } 
      // Handle DocumentPicker result
      else if ('assets' in result && result.assets) {
        const asset = result.assets[0];
        if (asset) {
          await uploadFile({
            uri: asset.uri,
            name: asset.name,
            type: asset.mimeType || 'application/octet-stream',
            size: asset.size || 0,
          });
        }
      }
    } catch (error) {
      console.error('Attachment picker error:', error);
      Alert.alert('Error', 'Failed to pick attachment');
    }
  };

  const uploadFile = async (file: { uri: string; name: string; type: string; size: number }) => {
    try {
      setUploading(true);
      setUploadProgress(0);

      // Get presigned upload URL
      const { attachmentId, uploadUrl, key, category } = await api.getUploadUrl({
        fileName: file.name,
        contentType: file.type,
        fileSize: file.size,
        conversationId,
      });

      // Fetch file as blob
      const response = await fetch(file.uri);
      const blob = await response.blob();

      // Upload to S3
      await api.uploadFile(uploadUrl, blob, file.type, (progress) => {
        setUploadProgress(progress);
      });

      // Create attachment object
      const attachment: Attachment = {
        id: attachmentId,
        key,
        fileName: file.name,
        contentType: file.type,
        fileSize: file.size,
        category,
      };

      onAttachmentReady(attachment, file.uri);
      onClose();
    } catch (error) {
      console.error('Upload error:', error);
      Alert.alert('Upload Failed', 'Failed to upload attachment. Please try again.');
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <TouchableOpacity 
        style={styles.overlay} 
        activeOpacity={1} 
        onPress={onClose}
      >
        <View style={styles.container}>
          {uploading ? (
            <View style={styles.uploadingContainer}>
              <ActivityIndicator size="large" color={colors.primary[500]} />
              <Text style={styles.uploadingText}>Uploading... {uploadProgress}%</Text>
              <View style={styles.progressBar}>
                <View style={[styles.progressFill, { width: `${uploadProgress}%` }]} />
              </View>
            </View>
          ) : (
            <>
              <Text style={styles.title}>Send Attachment</Text>
              
              <View style={styles.optionsRow}>
                <TouchableOpacity 
                  style={styles.option}
                  onPress={() => handleOption('camera')}
                >
                  <View style={[styles.optionIcon, { backgroundColor: colors.primary[600] }]}>
                    <Ionicons name="camera" size={28} color={colors.white} />
                  </View>
                  <Text style={styles.optionText}>Camera</Text>
                </TouchableOpacity>

                <TouchableOpacity 
                  style={styles.option}
                  onPress={() => handleOption('gallery')}
                >
                  <View style={[styles.optionIcon, { backgroundColor: colors.accent[600] }]}>
                    <Ionicons name="images" size={28} color={colors.white} />
                  </View>
                  <Text style={styles.optionText}>Gallery</Text>
                </TouchableOpacity>

                <TouchableOpacity 
                  style={styles.option}
                  onPress={() => handleOption('document')}
                >
                  <View style={[styles.optionIcon, { backgroundColor: colors.surface[600] }]}>
                    <Ionicons name="document" size={28} color={colors.white} />
                  </View>
                  <Text style={styles.optionText}>Document</Text>
                </TouchableOpacity>
              </View>

              <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
                <Text style={styles.cancelText}>Cancel</Text>
              </TouchableOpacity>
            </>
          )}
        </View>
      </TouchableOpacity>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  container: {
    backgroundColor: colors.surface[900],
    borderTopLeftRadius: borderRadius.xl,
    borderTopRightRadius: borderRadius.xl,
    padding: spacing.lg,
    paddingBottom: spacing.xxl,
  },
  title: {
    color: colors.white,
    fontSize: fontSize.lg,
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  optionsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: spacing.lg,
  },
  option: {
    alignItems: 'center',
    gap: spacing.sm,
  },
  optionIcon: {
    width: 60,
    height: 60,
    borderRadius: borderRadius.full,
    justifyContent: 'center',
    alignItems: 'center',
  },
  optionText: {
    color: colors.surface[300],
    fontSize: fontSize.sm,
  },
  cancelButton: {
    backgroundColor: colors.surface[800],
    paddingVertical: spacing.md,
    borderRadius: borderRadius.lg,
    alignItems: 'center',
  },
  cancelText: {
    color: colors.surface[300],
    fontSize: fontSize.md,
    fontWeight: '500',
  },
  uploadingContainer: {
    alignItems: 'center',
    paddingVertical: spacing.xl,
  },
  uploadingText: {
    color: colors.surface[300],
    fontSize: fontSize.md,
    marginTop: spacing.md,
    marginBottom: spacing.md,
  },
  progressBar: {
    width: '100%',
    height: 4,
    backgroundColor: colors.surface[700],
    borderRadius: 2,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: colors.primary[500],
  },
});

